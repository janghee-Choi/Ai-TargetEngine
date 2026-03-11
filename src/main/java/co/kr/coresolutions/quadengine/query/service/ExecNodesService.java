package co.kr.coresolutions.quadengine.query.service;

import co.kr.coresolutions.quadengine.common.event.SchedulePublisher;
import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.common.model.CommonResponse;
import co.kr.coresolutions.quadengine.common.util.DateUtils;
import co.kr.coresolutions.quadengine.common.util.HttpUtils;
import co.kr.coresolutions.quadengine.common.util.SqlUtils;
import co.kr.coresolutions.quadengine.query.domain.MonthType;
import co.kr.coresolutions.quadengine.query.domain.WeekType;
import co.kr.coresolutions.quadengine.query.executor.ExecNodes;
import co.kr.coresolutions.quadengine.query.model.ExecNodesRequest;
import co.kr.coresolutions.quadengine.query.model.ExecNodesResponse;
import co.kr.coresolutions.quadengine.query.model.dtos.DateValidation;
import co.kr.coresolutions.quadengine.query.model.dtos.NodeCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecNodesService {

	public static Map<String, ExecNodesResponse> cacheExecNodes = new ConcurrentHashMap<>();
	public static Map<String, String> nodeActionMap = new ConcurrentHashMap<>();

	private final QueryService queryService;
	private final SchedulePublisher schedulePublisher;
	private final ExecNodes execNodes;
	private final HttpUtils httpUtils;
	private final ObjectMapper objectMapper;

	public boolean isRunning(String runId) {
		return cacheExecNodes.containsKey(runId);
	}

	public List<ExecNodesResponse> findAll() {
		List<ExecNodesResponse> merged = new ArrayList<>();

		if (httpUtils.isRunningModeDuplex()) {
			String url = "/execNodes/locally";
			CommonResponse<List<ExecNodesResponse>> result = httpUtils.getQueryPartner(url, CommonResponse.class);

			if (result.getSuccess()) {
				List<ExecNodesResponse> partner = Optional.of(result)
						.map(CommonResponse::getData)
						.orElseGet(Collections::emptyList)
						.stream()
						.filter(Objects::nonNull)
						.toList();

				merged.addAll(partner);
			}
		}

		List<ExecNodesResponse> local = cacheExecNodes.values().stream().filter(Objects::nonNull).toList();
		merged.addAll(local);

		return merged;
	}

	public List<ExecNodesResponse> findAllLocally() {
		return cacheExecNodes.values().stream().filter(Objects::nonNull).toList();
	}

	public void execNodes(String campId, String stepId, ExecNodesRequest execNodesRequest, String urlPath) {
		String partMessage = "execNodes triggered (campId = " + campId + " , stepId = " + stepId + "), ";
		String message;

		String runId = execNodesRequest.getRunId();
		String userId = execNodesRequest.getUser();
		String source = execNodesRequest.getSource();
		String tableName = execNodesRequest.getTable();
		ExecNodesRequest.Conditions conditions = execNodesRequest.getConditions();

		if (runId == null || runId.isEmpty()) {
			runId = "RUNID_" + UUID.randomUUID();
		} else {
			if (cacheExecNodes.containsKey(runId)) {
				message = "execNodes API is requested ( runId is {" + runId + "} )  , but  not executed because same runid is found.";
				log.info("Method : {} , Message : {}", "POST", message);
				throw new CommonException(ErrorCode.EXEC_NODE_ALREADY_EXIST, message);
			}

			if (httpUtils.isRunningModeDuplex()) {
				String url = "/execNodes/isRunning/" + runId;
				CommonResponse<Boolean> result = httpUtils.getQueryPartner(url, CommonResponse.class);
				if (result.getSuccess() && result.getData()) {
					message = "Same runId (" + runId + ")  is running , so it's canceled";
					log.error("Method : {} , Message : {}", "POST", message);
					throw new CommonException(ErrorCode.COMMAND_ALREADY_EXIST, message);
				}
			}
		}

		cacheExecNodes.put(runId, ExecNodesResponse.builder()
				.runId(runId)
				.campId(campId)
				.stepId(stepId)
				.startTime(DateUtils.getNowStr())
				.userId(userId)
				.build());

		message = "execNodes API is requested ( runId is {" + runId + "} )";
		log.info("Method : {} , Message : {}", "POST", message);

		try {
			if (source != null && !(source.equals("body") || source.equals("batch_body"))) {
				log.info("Method : {} , Message : {}", "POST", partMessage + "valid value is one of  body  or batch_body, camp_id = " + campId);
				throw new CommonException(ErrorCode.REQUEST_BODY_NOT_VALID, "valid value is one of body or batch_body");
			}

			checkTodayAllowed(execNodesRequest.getDateValidation(), campId);

			String sqlCommand = """
					SELECT NODE_COMMAND
					  FROM %s
					 WHERE CAMP_ID = :campId
					   AND STEP_ID = :stepId
					 ORDER BY SEQ
					""".formatted(tableName);
			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("campId", campId)
					.addValue("stepId", stepId);
			List<Map<String, Object>> commandList = queryService.selectList(sqlCommand, params);
			if (commandList.isEmpty()) {
				throw new CommonException(ErrorCode.EXEC_NODE_IS_EMPTY);
			}
			List<NodeCommand> nodeCommandList = commandList.stream()
					.map(row -> objectMapper.convertValue(row, NodeCommand.class))
					.toList();

			Boolean batchSubmit = conditions.getBatchsubmit();
			Boolean retargeting = conditions.getRetargeting();
			Boolean submit = conditions.getSubmit();

			List<NodeCommand> finalCommandList = nodeCommandList.stream()
					.filter(command -> {
						if (batchSubmit != null
								&& !Objects.equals(batchSubmit, command.getBatchSubmit())) {
							return false;
						}

						if (retargeting != null
								&& !Objects.equals(retargeting, command.getRetargeting())) {
							return false;
						}

						if (submit != null
								&& !Objects.equals(submit, command.getSubmit())) {
							return false;
						}

						if (batchSubmit == null && retargeting == null) {
							return Boolean.TRUE.equals(command.getBatchSubmit());
						}

						return true;
					})
					.toList();

			String sqlSeq = """
					SELECT MAX(EXEC_SEQ) AS MAX_SEQ
					  FROM T_CAMP_NODE_EXEC_RESULT
					 WHERE CAMP_ID = :campId
					   AND STEP_ID = :stepId
					""";
			Map<String, Object> seqMap = queryService.selectOne(sqlSeq, params);
			BigDecimal maxSeq = SqlUtils.getBigDecimal(seqMap, "MAX_SEQ");
			Long seq = maxSeq == null ? 1 : maxSeq.longValue() + 1;

			execNodes.run(runId, seq, source, finalCommandList);
		} finally {
			cacheExecNodes.remove(runId);
		}
	}

	public boolean stopExecNodes(String runId) {
		log.info("Method : {} , Message : {}", "POST", "execNodes/{" + runId + "} request is terminated by \"DELETE\"");
		if (stopExecNodesLocal(runId)) {
			return true;
		}

		if (httpUtils.isRunningModeDuplex()) {
			String url = "/stopExecNodes/local/" + runId + "/delete";
			CommonResponse<Boolean> result = httpUtils.postQueryPartner(url, null, CommonResponse.class);
			return result.getSuccess() && result.getData();
		}

		return false;
	}

	public boolean stopExecNodesLocal(String runId) {
		log.info("Method : {} , Message : {}", "POST", "stopExecNodes/local/{" + runId + "} triggered");
		execNodes.stop(runId);
		ExecNodesResponse execNodesResponse = cacheExecNodes.remove(runId);
		return execNodesResponse != null;
	}

	private void checkTodayAllowed(DateValidation dateValidation, String campId) {
		LocalDate today = LocalDate.now();
		MonthType monthType = MonthType.from(today.getMonth());
		WeekType weekType = WeekType.from(today.getDayOfWeek());

		if (!isMonthTrue(dateValidation.getMonth(), monthType)) {
			throw new CommonException(ErrorCode.EXEC_NODE_DATE_NOT_VALID, "Month is false");
		}

		if (!isWeekTrue(dateValidation.getWeek(), weekType)) {
			throw new CommonException(ErrorCode.EXEC_NODE_DATE_NOT_VALID, "Week is false");
		}

		if (Boolean.TRUE.equals(dateValidation.getValidateHoliday()) && checkHoliday(campId)) {
			throw new CommonException(ErrorCode.EXEC_NODE_DATE_NOT_VALID, "Today is holiday");
		}
	}

	private boolean isMonthTrue(DateValidation.Month month, MonthType monthType) {
		String value = switch (monthType) {
			case JAN -> month.getJan();
			case FEB -> month.getFeb();
			case MAR -> month.getMar();
			case APR -> month.getApr();
			case MAY -> month.getMay();
			case JUN -> month.getJun();
			case JUL -> month.getJul();
			case AUG -> month.getAug();
			case SEP -> month.getSep();
			case OCT -> month.getOct();
			case NOV -> month.getNov();
			case DEC -> month.getDec();
		};

		return "true".equalsIgnoreCase(value);
	}

	private boolean isWeekTrue(DateValidation.Week week, WeekType weekType) {
		String value = switch (weekType) {
			case SUN -> week.getSun();
			case MON -> week.getMon();
			case TUE -> week.getTue();
			case WED -> week.getWed();
			case THU -> week.getThu();
			case FRI -> week.getFri();
			case SAT -> week.getSat();
		};

		return "true".equalsIgnoreCase(value);
	}

	private boolean checkHoliday(String holidayTableName) {
		String dateStr = DateUtils.getNowStr();
		String sql = """
				SELECT HLDY_NM
				  FROM %s
				 WHERE HLDY_YR = :hldyYr
				   AND HLDY_MN = :hldyMn
				   AND HLDY_DT = :hldyDt
				""".formatted(holidayTableName.toUpperCase());
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("hldyYr", dateStr.substring(0, 4))
				.addValue("hldyMn", dateStr.substring(4, 6))
				.addValue("hldyDt", dateStr.substring(6, 8));
		List<Map<String, Object>> holidayList = queryService.selectList(sql, params);

		return !holidayList.isEmpty();
	}

}
