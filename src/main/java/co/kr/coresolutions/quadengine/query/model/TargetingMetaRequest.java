package co.kr.coresolutions.quadengine.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TargetingMetaRequest {

	@NotBlank
	@JsonProperty("queryType")
	private String queryType;

	@JsonProperty("authList")
	private List<Integer> authList;

	@NotBlank
	@JsonProperty("owner")
	private String owner;

	@JsonProperty("campId")
	private String campId;

}
