package co.kr.coresolutions.quadengine.query.outbound.connection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class KafkaConnection {
    @JsonProperty(required = true)
    private String channel;
    @JsonProperty(required = true)
    private String ip;
    @JsonProperty(required = true)
    private String topic;
}
