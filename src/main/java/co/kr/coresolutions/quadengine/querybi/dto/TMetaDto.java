package co.kr.coresolutions.quadengine.querybi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import co.kr.coresolutions.quadengine.querybi.dto.TMetaResultDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import co.kr.coresolutions.quadengine.querybi.interfaces.KeyedResult;

@Getter
@Setter
@Builder
public class TMetaDto implements KeyedResult {
    @JsonProperty("sessionid")
    private String sessionId;

    @JsonProperty("audience_id")
    private String audienceId;

    private List<TMetaResultDto> tMetaResultList;

    private String keyId;
    private String result;
}
