package co.kr.coresolutions.quadengine.query.controller;

import co.kr.coresolutions.quadengine.query.service.MultiLevelService;
import co.kr.coresolutions.quadengine.query.model.MultiLevelCategoryRequest;
import co.kr.coresolutions.quadengine.query.model.MultiLevelCategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

@Tag(name = "MultiLevel", description = "멀티레벨 카테고리/검색 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(QueryRoutes.QUERY_BASE_URL + "/multilevel")
public class MultiLevelController {

	private final MultiLevelService multiLevelService;

	@Operation(
		summary = "멀티레벨 카테고리 조회",
		description = "connectionId와 요청 바디(MultiLevelCategoryRequest)를 기반으로 멀티레벨 카테고리 결과를 조회합니다."
	)
	@PostMapping("/{connectionId}/category")
	public MultiLevelCategoryResponse selectMlCategory(
			@Parameter(description = "대상 커넥션 ID", required = true, example = "CONN-001")
			@PathVariable String connectionId,
			@RequestBody MultiLevelCategoryRequest multiLevelCategoryRequest
	) {
		return multiLevelService.selectMlCategory(multiLevelCategoryRequest, connectionId);
	}

	@Operation(
		summary = "멀티레벨 검색",
		description = "connectionId와 검색 조건(JsonNode)을 기반으로 멀티레벨 검색 결과를 조회합니다."
	)
	@PostMapping("/{connectionId}/search")
	public Map<String, Object> selectMlSearch(
			@Parameter(description = "대상 커넥션 ID", required = true, example = "CONN-001")
			@PathVariable String connectionId,
			@RequestBody JsonNode jsonNode
	) {
		return multiLevelService.selectMlSearch(jsonNode, connectionId);
	}

}
