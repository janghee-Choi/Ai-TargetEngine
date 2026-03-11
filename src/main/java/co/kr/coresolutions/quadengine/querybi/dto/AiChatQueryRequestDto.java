package co.kr.coresolutions.quadengine.querybi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatQueryRequestDto {
    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("targetRowCount")
    private long targetRowCount;
}
