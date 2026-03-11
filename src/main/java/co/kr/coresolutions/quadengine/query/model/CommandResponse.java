package co.kr.coresolutions.quadengine.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommandResponse {

	@JsonProperty("CommandID")
	String commandId;

	@JsonProperty("commadName")
	String commandName;

	@JsonProperty("startdatetime")
	String startDateTime;

	String owner;

}
