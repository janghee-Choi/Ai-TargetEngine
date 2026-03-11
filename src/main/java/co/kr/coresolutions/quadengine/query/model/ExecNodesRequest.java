package co.kr.coresolutions.quadengine.query.model;

import co.kr.coresolutions.quadengine.query.model.dtos.DateValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExecNodesRequest {

	@NotBlank
	@JsonProperty("connectionid")
	private String connectionId;

	@NotBlank
	private String table;

	@NotBlank
	private String user;

	@Valid
	@JsonProperty("datevalidation")
	private DateValidation dateValidation;

	@Valid
	@JsonProperty("conditions")
	private Conditions conditions;

	@JsonProperty
	private String source;

	@JsonProperty(value = "runid")
	private String runId;

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Conditions {
		Boolean submit;
		Boolean batchsubmit;
		Boolean retargeting;
	}
}
