package co.kr.coresolutions.quadengine.querybi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TMetaResultDto {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("queryid")
    private String queryId;
        
    @JsonProperty("filterid")
    private String filterId;
    
    @JsonProperty("operator")
    private String operator;
    @JsonProperty("value")
    private String value;
    
    @JsonProperty("valuename")
    private String valueName;
        
    @JsonProperty("desc")
    private String desc;
    @JsonProperty("required")
    private boolean required;
    @JsonProperty("question")
    private String question;
    
    @JsonProperty("success")
    private boolean success;
}
