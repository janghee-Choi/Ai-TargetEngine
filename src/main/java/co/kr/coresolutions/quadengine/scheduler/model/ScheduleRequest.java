package co.kr.coresolutions.quadengine.scheduler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ScheduleRequest {

	@JsonProperty(required = true)
	private String scId;

	@JsonProperty(required = true)
	private String startDttm;

	@JsonProperty(required = true)
	private String endDttm;

	@JsonProperty(required = true)
	private String name;

	@JsonProperty(required = true)
	private String schedule;

	@JsonProperty(required = true)
	private String callMode;

	@JsonProperty(required = true)
	private String callString;

	@DateTimeFormat(pattern = "yyyyMMddHHmmss")
	@JsonProperty(required = true)
	private String created;

	@DateTimeFormat(pattern = "yyyyMMddHHmmss")
	@JsonProperty(required = true)
	private String updated;

	@JsonProperty(required = true)
	private String owner;

	@JsonProperty(required = true)
	@Valid
	private JsonNode httpBody;

	private String next;

}
