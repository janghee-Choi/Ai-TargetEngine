package co.kr.coresolutions.quadengine.querybi.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import co.kr.coresolutions.quadengine.querybi.interfaces.AiTargetingCodeInterface;
import lombok.Getter;

@Getter
public enum AiChatErrorCode implements AiTargetingCodeInterface {

	// 공통
	INVALID_REQUEST(400, "유효하지 않은 요청입니다."), INTERNAL_ERROR(500, "내부 서버 오류가 발생했습니다."),

	// 세션
	SESSION_NOT_FOUND(1001, "세션을 찾을 수 없습니다."),

	// Audience
	AUDIENCE_NOT_FOUND(1010, "Audience 데이터가 존재하지 않습니다."), AUDIENCE_PARSE_ERROR(1011, "Audience 파싱 중 오류가 발생했습니다."),

	// TMeta
	TMETA_NOT_FOUND(1020, "TMeta 데이터가 존재하지 않습니다."), TMETA_PARSE_ERROR(1021, "TMeta 파싱 중 오류가 발생했습니다."),
	TMETA_FAILURE(1022, "TMeta 처리 중 오류가 발생했습니다."),

	// Targeting
	TARGETING_NOT_FOUND(1030, "Targeting 결과가 존재하지 않습니다."), TARGETING_FAILED(1031, "Targeting 처리 중 오류가 발생했습니다."),
	TARGETING_INVALID(1032, "Targeting 데이터가 유효하지 않습니다."),

	// Query Builder
	QUERY_BUILD_ERROR(1040, "쿼리 생성 중 오류가 발생했습니다."), UNSUPPORTED_OPERATION(1041, "지원되지 않는 연산입니다.");

	private final Integer code;
	private final String message;

	AiChatErrorCode(Integer code, String message) {
		this.code = code;
		this.message = message;
	}

	@JsonValue
	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("code", code);
		map.put("message", message);
		return map;
	}
}