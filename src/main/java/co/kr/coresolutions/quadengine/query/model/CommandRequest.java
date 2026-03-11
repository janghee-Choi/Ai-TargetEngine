package co.kr.coresolutions.quadengine.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.node.ObjectNode;


@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CommandRequest {

	@NotBlank
	@JsonProperty("Command")
	private String command;

	@NotBlank
	@JsonProperty("Output")
	private String output;

	@JsonProperty("CommandId")
	private String commandId;

	private ObjectNode parameter;

	private String owner;

	@JsonProperty(value = "userid")
	private String userId;

	private String runMode = "INTERNAL";

}
