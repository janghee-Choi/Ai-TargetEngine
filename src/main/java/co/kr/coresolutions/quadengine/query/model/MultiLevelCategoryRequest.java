package co.kr.coresolutions.quadengine.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MultiLevelCategoryRequest {

	@NotBlank
	@JsonProperty("@@ML_NAME@@")
	private String mlName;

	@NotBlank
	private int level;

	@NotBlank
	private String regexYn;

	@NotBlank
	private String searchYn;

	private JsonNode replaceParams;

}
