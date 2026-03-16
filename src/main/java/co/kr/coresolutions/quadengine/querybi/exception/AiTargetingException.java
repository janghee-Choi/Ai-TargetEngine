package co.kr.coresolutions.quadengine.querybi.exception;

import co.kr.coresolutions.quadengine.querybi.enums.AiChatErrorCode;
import lombok.Getter;

@Getter
public class AiTargetingException extends RuntimeException {
    private final AiChatErrorCode errorCode;

    public AiTargetingException(AiChatErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // {} 가변 파라미터 포맷팅 지원
    public AiTargetingException(AiChatErrorCode errorCode, String messageTemplate, Object... args) {
        super(format(messageTemplate, args));
        this.errorCode = errorCode;
    }

    private static String format(String template, Object... args) {
        String result = template;
        for (Object arg : args) {
            result = result.replaceFirst("\\{}", arg != null ? arg.toString() : "null");
        }
        return result;
    }

}