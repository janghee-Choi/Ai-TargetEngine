package co.kr.coresolutions.quadengine.scheduler.validation;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.scheduler.model.ScheduleRequest;
import co.kr.coresolutions.quadengine.scheduler.util.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.Locale;

@Component
public class ScheduleRequestValidator {

	public void validate(ScheduleRequest req) throws ParseException {
		if (req == null) {
			throw badRequest("ScheduleRequest is required.");
		}

		// (1) httpBody 사이즈 체크 size() > 0 and JSON Format 인지 체크
		// - JsonNode로 바인딩되었으면 JSON 파싱은 성공한 것
		// - 여기서는 null/빈 오브젝트/빈 배열, 그리고 Object/Array 외 타입을 거절
		if (req.getHttpBody() == null || req.getHttpBody().isNull()) {
			throw badRequest("httpBody is required.");
		}
		if (!(req.getHttpBody().isObject() || req.getHttpBody().isArray())) {
			throw badRequest("httpBody must be a JSON object or array.");
		}
		if (req.getHttpBody().size() <= 0) {
			throw badRequest("httpBody must not be empty.");
		}

		// (2) callMode : GET, POST 허용
		if (!StringUtils.hasText(req.getCallMode())) {
			throw badRequest("callMode is required.");
		}
		String callMode = req.getCallMode().trim().toUpperCase(Locale.ROOT);
		if (!("GET".equals(callMode) || "POST".equals(callMode))) {
			throw badRequest("callMode must be GET or POST.");
		}

		// (3) startDttm, endDttm 체크 isValidDateTime, getCronExpOrDate, isValidDates
		String normalizedStart = req.getStartDttm();
		String normalizedEnd = req.getEndDttm();
		if (!StringUtils.hasText(req.getStartDttm()) || !StringUtils.hasText(req.getEndDttm())) {
			throw badRequest("startDttm and endDttm are required.");
		}
		if (!DateUtils.isValidDateTime(req.getStartDttm())) {
			normalizedStart = DateUtils.getCronExpOrDate(req.getStartDttm(), false);
			if (normalizedStart.isEmpty()) {
				throw badRequest("startDttm is invalid.");
			}
		}
		if (!DateUtils.isValidDateTime(req.getEndDttm())) {
			normalizedEnd = DateUtils.getCronExpOrDate(req.getEndDttm(), false);
			if (normalizedEnd.isEmpty()) {
				throw badRequest("endDttm is invalid.");
			}
		}

		if (!DateUtils.isValidDates(normalizedStart, normalizedEnd)) {
			throw badRequest("startDttm must be earlier than endDttm.");
		}

		// (4) scheduleCron 체크 isCronExp, getCronExpOrDate
		if (!StringUtils.hasText(req.getSchedule())) {
			throw badRequest("schedule is required.");
		}
		String cronExp = req.getSchedule();
		if (!DateUtils.isCronExp(req.getSchedule())) {
			cronExp = DateUtils.getCronExpOrDate(req.getSchedule(), true);
			if (cronExp.isEmpty()) {
				throw badRequest("schedule is not a valid cron expression.");
			}
		}

		// (5) 스케줄 시간이 start, end 사이에 있는지 체크 isCronExpBetweenRanges
		DateUtils dateUtils = new DateUtils();
		boolean ok = dateUtils.isCronExpBetweenRanges(normalizedStart, normalizedEnd, cronExp);
		if (!ok) {
			throw badRequest("schedule cron time must be between startDttm and endDttm.");
		}

		// (6) 스케줄ID(scId)가 httpBody내의 있는지 없는지 확인인
		if (req.getScId().trim().isEmpty()) {
			throw badRequest("httpBody in scId is Empty");
		}
	}

	private CommonException badRequest(String message) {
		return new CommonException(ErrorCode.REQUEST_BODY_NOT_VALID, message);
	}
}