package co.kr.coresolutions.quadengine.query.outbound.connection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class S3Connection {
    @JsonProperty(required = true)
    private String bucket;
    @JsonProperty(required = true)
    private String region;
    @JsonProperty(required = true)
    private String iamRole;
    @JsonProperty(required = true)
    private String accessKey;
    @JsonProperty(required = true)
    private String secretKey;
    @JsonProperty(required = true)
    private boolean awsCredentials;
    @JsonProperty(required = true)
    private String redshiftCredentials;
}
