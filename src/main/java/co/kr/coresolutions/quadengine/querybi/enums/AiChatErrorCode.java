package co.kr.coresolutions.quadengine.querybi.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import co.kr.coresolutions.quadengine.common.exception.CodeInterface;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum AiChatErrorCode implements CodeInterface {

    AI_CHAT_INVALID_REQUEST(100, "유효하지 않은 요청입니다."),
    AI_CHAT_PROCESSING_ERROR(101, "AI Chat 처리 중 오류가 발생했습니다."),
    AI_CHAT_UNSUPPORTED_OPERATION(102, "지원되지 않는 연산입니다."),
    AI_CHAT_AUDIENCE_PARSING_ERROR(103, "Audience 파싱 중 오류가 발생했습니다."),
    AI_CHAT_TMETA_PARSING_ERROR(104, "Meta 파싱 중 오류가 발생했습니다."),
    AI_CHAT_QUERY_GENERATION_ERROR(104, "쿼리 생성 중 오류가 발생했습니다."),
    
    // AI Chat Query Builder 에러
    AI_CHAT_QUERY_BUILDER_ERROR(1000, "AI Chat Query Builder 에러"),
        
    // AI Chat Query Targeting 에러
	AI_CHAT_QUERY_TARGETING_ERROR(1001, "AI Chat Query Targeting 에러"),
    AI_CHAT_QUERY_TARGETING_NOT_FOUND(1002, "AI Chat Query Targeting 데이터가 존재하지 않습니다."),
    AI_CHAT_QUERY_TARGETING_INVALID(1003, "AI Chat Query Targeting 데이터가 유효하지 않습니다."),
    AI_CHAT_QUERY_TARGETING_PROCESSING_ERROR(1004, "AI Chat Query Targeting 처리 중 오류가 발생했습니다."),


	// 기타오류
	FAILED(999, "FAILED");


	private final Boolean success = false;
	private final Integer code;
	private final String message;

	@JsonValue
	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("success", success);
		map.put("code", code);
		map.put("message", message);
		return map;
	}
}
