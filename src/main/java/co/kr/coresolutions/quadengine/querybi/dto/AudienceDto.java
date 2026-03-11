package co.kr.coresolutions.quadengine.querybi.dto;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AudienceDto {
    
    private String id;
    
    @JsonProperty("audience_parsed")
    private String audienceParsed;
    
    private List<ConditionDto> conditions;

    private SetOperationDto setOperation;

    public AudienceDto() {
    }

    public AudienceDto(String id, String audienceParsed, List<ConditionDto> conditions, SetOperationDto setOperation) {
        this.id = id;
        this.audienceParsed = audienceParsed;
        this.conditions = conditions;
        this.setOperation = setOperation;
    }
}
