package co.kr.coresolutions.quadengine.querybi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import co.kr.coresolutions.quadengine.querybi.dto.TMetaResultDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TMetaDto {
    @JsonProperty("sessionid")
    private String sessionId;
    
    @JsonProperty("audience_id")
    private String audienceId;
        
    private List<TMetaResultDto> tMetaResultList;
    
}
