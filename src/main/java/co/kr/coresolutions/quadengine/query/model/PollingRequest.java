package co.kr.coresolutions.quadengine.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PollingRequest {

	@NotBlank
	private String dpId;

	//TODO 제거 확인 필요
	@NotBlank
	private String connectionId;

	//TODO 제거 확인 필요
	@NotBlank
	private String user;

}
