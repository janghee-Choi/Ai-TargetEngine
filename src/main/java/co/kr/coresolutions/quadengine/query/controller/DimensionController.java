package co.kr.coresolutions.quadengine.query.controller;

import co.kr.coresolutions.quadengine.query.service.DimensionService;
import co.kr.coresolutions.quadengine.query.model.QueryResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;

@Tag(name = "Dimension", description = "Dimension 기반 조회 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(QueryRoutes.QUERY_BASE_URL)
public class DimensionController {

	private final DimensionService dimensionService;

	@Operation(
		summary = "Dimension 조회(GET)",
		description = "connectionId, langType, dimensionName 기반으로 dimension 쿼리를 수행합니다. (치환값 없음)"
	)
	@GetMapping("/{connectionId}/dimension/{langType}/{dimensionName}")
	public QueryResultResponse queryWithDimension(
			@Parameter(description = "대상 커넥션 ID", required = true, example = "CONN-001")
			@PathVariable String connectionId,
			@Parameter(description = "언어 타입", required = true, example = "ko")
			@PathVariable String langType,
			@Parameter(description = "Dimension 이름", required = true, example = "product")
			@PathVariable String dimensionName
	) {
		return dimensionService.queryWithDimension(connectionId, langType, dimensionName, null);
	}

	@Operation(
		summary = "Dimension 조회(POST)",
		description = "connectionId, langType, dimensionName 기반으로 dimension 쿼리를 수행합니다. (치환값 JSON 제공)"
	)
	@PostMapping("/{connectionId}/dimension/{langType}/{dimensionName}")
	public QueryResultResponse postWithDimension(
			@Parameter(description = "대상 커넥션 ID", required = true, example = "CONN-001")
			@PathVariable String connectionId,
			@Parameter(description = "언어 타입", required = true, example = "ko")
			@PathVariable String langType,
			@Parameter(description = "Dimension 이름", required = true, example = "product")
			@PathVariable String dimensionName,
			@RequestBody @Valid JsonNode replaceNode
	) {
		return dimensionService.queryWithDimension(connectionId, langType, dimensionName, replaceNode);
	}
}
