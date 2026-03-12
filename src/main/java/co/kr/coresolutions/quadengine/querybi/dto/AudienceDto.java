package co.kr.coresolutions.quadengine.querybi.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudienceDto {
    @JsonProperty("set_operation")
    private SetOperationDto setOperation;

    @JsonProperty("audiences")
    private List<AudienceDetailDto> audienceDetailList;
}
