package co.kr.coresolutions.quadengine.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;


@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NodeWorkRequest {

	@NotBlank
	@JsonProperty("projectid")
	private String campId;

	@NotNull
	private JsonNode nodes;

}
