package co.kr.coresolutions.quadengine.querybi.dto;

import java.util.List;

import co.kr.coresolutions.quadengine.querybi.dto.AudienceDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import co.kr.coresolutions.quadengine.querybi.dto.SetOperationDto;
import lombok.Getter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatQueryResponseDto {

    // 처리 결과
    private boolean success;
    private String message;

    // 타겟팅 결과
    private String rowCount;
    private String query;

    // 대상 정보
    @JsonProperty("audience")
    private AudienceDto audience;
    private SetOperationDto setOperation;

    // 실패 시 에러 상세 (성공 시 null)
    private ErrorDetail error;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String detail;
    }

    public static AiChatQueryResponseDto success(String rowCount, String query, AudienceDto audience,
            SetOperationDto setOperation) {
        return AiChatQueryResponseDto.builder().success(true).message("Audience targeting completed successfully")
                .rowCount(rowCount).query(query).audience(audience).setOperation(setOperation).build();
    }

    public static AiChatQueryResponseDto failure(String code, String detail) {
        return AiChatQueryResponseDto.builder().success(false).message("Audience targeting failed")
                .error(ErrorDetail.builder().code(code).detail(detail).build()).build();
    }
}