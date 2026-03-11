package co.kr.coresolutions.quadengine.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TargetingMetaRequest {

	@NotBlank
	private String queryType;

	private List<Integer> authList;

	@NotBlank
	private String owner;

	private String campId;

}
