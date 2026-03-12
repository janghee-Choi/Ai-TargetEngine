package co.kr.coresolutions.quadengine.querybi.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudienceDetailDto {
    private String id; // "A", "B"
    @JsonProperty("audience_parsed")
    private String audienceParsed; // "20대 여성"
    @JsonProperty("conditions")
    private List<ConditionDto> conditions;
}
