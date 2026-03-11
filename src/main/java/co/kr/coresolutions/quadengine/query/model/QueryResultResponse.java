package co.kr.coresolutions.quadengine.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;


@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResultResponse {

	@JsonProperty("Input")
	private List<Map<String, Object>> input;

	@JsonProperty("Fields")
	private List<Field> meta;

	@Getter
	@AllArgsConstructor
	public static class Field {

		@JsonProperty("FieldName")
		String fieldName;

		@JsonProperty("FieldDesc")
		String fieldDesc;

		@JsonProperty("FieldType")
		String fieldType;

	}
}
