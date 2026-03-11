package co.kr.coresolutions.quadengine.query.service;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.common.util.DateUtils;
import co.kr.coresolutions.quadengine.common.util.SqlUtils;
import co.kr.coresolutions.quadengine.query.domain.PollingTarget;
import co.kr.coresolutions.quadengine.query.domain.PollingTaskResult;
import co.kr.coresolutions.quadengine.query.executor.PollingExecutor;
import co.kr.coresolutions.quadengine.query.model.PollingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class PollingService {

	public static Set<String> cachePolling = ConcurrentHashMap.newKeySet();

	private final QueryService queryService;
	private final PollingExecutor pollingExecutor;
	private final ObjectMapper objectMapper;
	private final Executor pollingTaskExecutor;

	public void polling(PollingRequest pollingRequest) {
		String dpId = pollingRequest.getDpId();

		if (cachePolling.contains(dpId)) {
			String message = "Same DP_ID {" + dpId + "} is running";
			errorLogging(dpId, message);
			throw new CommonException(ErrorCode.POLLING_ALREADY_RUNNING, "Same DP_ID {" + dpId + "} is running");
		}

		cachePolling.add(dpId);

		try {
			String sqlDp = """
					SELECT DP.DP_ID
						 , DP.DP_QRY
						 , DP.DP_ORG_CONN_INFO
						 , DP.DP_SEQ_FIELD_NM
						 , DP.DP_LAST_SEQ
						 , DP.DP_MAX_RCRD_CNT
					  FROM T_DBPOLLING DP
					 WHERE DP.DP_ID = :dpId
					""";
			MapSqlParameterSource param = new MapSqlParameterSource().addValue("dpId", dpId);
			Map<String, Object> dbPolling = queryService.selectOne(sqlDp, param);
			// dbPolling 조회결과가 없을때 에러 처리 ErrorCode.POLLING_NOT_FOUND
			if (dbPolling == null) {
				String message = "fail due to polling with id\t" + dpId + "\t doesn't exist";
				errorLogging(dpId, message);
				throw new CommonException(ErrorCode.POLLING_NOT_FOUND, message);
			}

			String dpQuery = Objects.toString(dbPolling.get("DP_QRY"), null);
			String dpInConnId = Objects.toString(dbPolling.get("DP_ORG_CONN_INFO"), null);
			String dpSqlField = Objects.toString(dbPolling.get("DP_SEQ_FIELD_NM"), null);
			BigDecimal dbLastSeq = SqlUtils.getBigDecimal(dbPolling, "DP_LAST_SEQ");
			BigDecimal dpMaxRecords = SqlUtils.getBigDecimal(dbPolling, "DP_MAX_RCRD_CNT");

			BigDecimal lastSeqProcessed = dbLastSeq != null ? dbLastSeq : BigDecimal.ZERO;
			BigDecimal lastSeqProPlusMax = lastSeqProcessed.add(dpMaxRecords);

			String sqlList = """
					SELECT TRGT.DP_ID
						 , TRGT.DP_OTPT_SEQ
						 , TRGT.DP_OTPT_TYPE
						 , TRGT.DP_TRGT_TYPE
						 , TRGT.DP_TBL_NM
						 , TRGT.DP_SAVE_MTHD
						 , TRGT.DP_TBL_KEY
						 , TRGT.DP_FILE_DLM
						 , TRGT.DP_FILE_CRT_CYCLE
						 , TRGT.DP_TRGT_CONN_INFO
						 , COMM.VAL3 AS DP_DML_SQL
						 , TRGT.DP_MID_CONN_INFO
					  FROM T_DBPOLLING_TARGET TRGT
					 INNER JOIN T_LOOKUP_VALUE COMM
					    ON COMM.LANG = 'ko_KR'
					   AND COMM.DEL_F = 'N'
					   AND COMM.LOOKUP_TYPE_ID = 'DP_TRGT_TYPE'
					   AND TRGT.DP_TRGT_TYPE = COMM.LOOKUP_CODE
					 WHERE TRGT.DP_ID = :dpId
					""";
			List<Map<String, Object>> targetList = queryService.selectList(sqlList, param);

			if (targetList.isEmpty()) {
				String message = "fail due to polling with id\t" + dpId + "\t target doesn't exist";
				errorLogging(dpId, message);
				throw new CommonException(ErrorCode.POLLING_TARGET_IS_EMPTY, message);
			}

			// 원천 쿼리 실행
			String dbQuery = dpQuery.replace("{LAST_SEQ_PROCESSED}", ":LAST_SEQ_PROCESSED")
					.replace("{LAST_SEQ_PROCESSED_PLUS_MAX}", ":LAST_SEQ_PROCESSED_PLUS_MAX");

			MapSqlParameterSource param2 = new MapSqlParameterSource().addValue("LAST_SEQ_PROCESSED", lastSeqProcessed)
					.addValue("LAST_SEQ_PROCESSED_PLUS_MAX", lastSeqProPlusMax);
			List<Map<String, Object>> originList = queryService.loadWithLimits(dpInConnId, dbQuery, param2,
					dpMaxRecords.intValue());
			BigDecimal maxSeqNumber = getMaxSeqNumber(originList, dpSqlField);

			List<PollingTarget> targets = targetList.stream()
					.map(m -> objectMapper.convertValue(m, PollingTarget.class)).toList();
			List<CompletableFuture<PollingTaskResult>> futures = targets.stream()
					.map(t -> CompletableFuture.supplyAsync(
							() -> pollingExecutor.executeTarget(t, originList, dpSqlField), pollingTaskExecutor))
					.toList();

			List<PollingTaskResult> results = futures.stream().map(CompletableFuture::join).toList();

			boolean anySuccess = results.stream().anyMatch(PollingTaskResult::success);
			if (anySuccess) {
				String sqlUpdate = """
						UPDATE T_DBPOLLING
						   SET DP_LAST_SEQ = :dpLastSeq
						 WHERE DP_ID = :dpId
						""";
				MapSqlParameterSource paramUpdate = new MapSqlParameterSource().addValue("dpLastSeq", maxSeqNumber)
						.addValue("dpId", dpId);
				queryService.update(sqlUpdate, paramUpdate);
			}
		} catch (Exception e) {
			throw new CommonException(ErrorCode.FAILED);
		} finally {
			cachePolling.remove(dpId);
		}
	}

	public BigDecimal getMaxSeqNumber(List<Map<String, Object>> list, String dbSqlField) {
		return list.stream().map(row -> SqlUtils.getBigDecimal(row, dbSqlField)).max(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);
	}

	private void errorLogging(String dpId, String errorMessage) {
		String partMessage = "dbpolling triggered (ID = " + dpId + " ), ";
		log.error("Method : {} , Message : {}", "POST", partMessage + "\tfail,\t" + errorMessage);
		logToDBTriggered(dpId, 0, false, BigDecimal.valueOf(-1), BigDecimal.ZERO, errorMessage, 0);
	}

	private void logToDBTriggered(String dpId, Integer dpOutSeq, Boolean success, BigDecimal lastSeqProc,
			BigDecimal lastSeqProcPlusMax, String outMessage, int records) {
		String sqlLog = """
				INSERT INTO T_DBPOLLING_TRIGGER (DP_ID, DP_OTPT_SEQ, DP_STS_CD, DP_LAST_START_SEQ, DP_LAST_END_SEQ, DP_MSG, LOAD_DTTM)
				VALUES (:dpId, :dpOutSeq, :dpStsCd, :dpLastStartSeq, :dpLastEndSeq, :dpMsg, :loadDttm)
				""";
		MapSqlParameterSource param = new MapSqlParameterSource().addValue("dpId", dpId).addValue("dpOutSeq", dpOutSeq)
				.addValue("dpStsCd", success ? "success" : "fail")
				.addValue("dpLastStartSeq", lastSeqProc.add(BigDecimal.ONE))
				.addValue("dpLastEndSeq", lastSeqProcPlusMax)
				.addValue("dpMsg", success ? "success \r(records:" + String.format("%,d", records) + ")"
						: outMessage.replaceAll("'", ""))
				.addValue("loadDttm", DateUtils.getNowStr());
		queryService.update(sqlLog, param);
	}

}
