package co.kr.coresolutions.quadengine.query.model.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NodeCommand {

	private Boolean submit;

	@JsonProperty("batchsubmit")
	private Boolean batchSubmit;

	private Boolean retargeting;

	private String url;

	private String nodeId;

	private String nodeType;

	private String stepId;

	private JsonNode body;

	@JsonProperty("batch_body")
	private JsonNode batchBody;
}
