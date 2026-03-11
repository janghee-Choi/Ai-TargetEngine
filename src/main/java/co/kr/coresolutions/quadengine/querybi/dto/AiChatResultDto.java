package co.kr.coresolutions.quadengine.querybi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiChatResultDto {
    @JsonProperty("sessionid")
    private String sessionId;

    @JsonProperty("keyid")
    private String keyId;

    @JsonProperty("audience_id")
    private String audienceId;

    @JsonProperty("userid") 
    private String userId;
    
    @JsonProperty("result")
    private String result;

    @JsonProperty("version")
    private String version;
}
