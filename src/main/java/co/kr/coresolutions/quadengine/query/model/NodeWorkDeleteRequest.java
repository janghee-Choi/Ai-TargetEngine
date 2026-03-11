package co.kr.coresolutions.quadengine.query.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NodeWorkDeleteRequest {

	@NotBlank
	private String campId;

	@NotBlank
	private String nodeId;

	@NotBlank
	private String tableName;

	@NotBlank
	private String connId;

	@NotBlank
	private String workConnId;

}
