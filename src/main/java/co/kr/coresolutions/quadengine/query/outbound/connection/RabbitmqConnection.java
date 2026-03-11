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
public class RabbitmqConnection {
    @JsonProperty(required = true)
    private String ip;
    @JsonProperty(required = true)
    private String port;
    @JsonProperty(required = true)
    private String id;
    @JsonProperty(required = true)
    private String pw;
    @JsonProperty(required = true)
    private String destination;
    @JsonProperty(required = true)
    private String queue;
    @JsonProperty(required = true)
    private String exchangeQueue;
    @JsonProperty(required = true)
    private String topic;
}