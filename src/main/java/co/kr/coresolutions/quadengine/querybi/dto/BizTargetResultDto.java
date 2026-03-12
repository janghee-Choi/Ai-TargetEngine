package co.kr.coresolutions.quadengine.querybi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import co.kr.coresolutions.quadengine.querybi.dto.TMetaResultDto;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
public class BizTargetResultDto {
    @JsonProperty("sessionid")
    private String sessionId;

    @JsonProperty("audience_id")
    private String audienceId;

    private List<TMetaResultDto> tMetaResultList;

    private boolean targetSuccess;
    private String query;
    private String rowCnt;
    private String outTableName;
}
