package co.kr.coresolutions.quadengine.query.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultiLevelCategoryResponse {

	@JsonUnwrapped
	private QueryResultResponse queryResultResponse;

	private String mlOp;

	private String mlQryNm;

	private String lastYn;

	private String searchYn;

}
