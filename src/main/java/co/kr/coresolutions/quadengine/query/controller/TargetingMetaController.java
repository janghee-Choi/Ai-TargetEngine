package co.kr.coresolutions.quadengine.query.controller;

import co.kr.coresolutions.quadengine.common.model.CommonResponse;
import co.kr.coresolutions.quadengine.query.model.TargetingMetaRequest;
import co.kr.coresolutions.quadengine.query.service.TargetingMetaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Tag(name = "TargetingMeta", description = "타겟팅 메타 조회/생성 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(QueryRoutes.QUERY_BASE_URL)
public class TargetingMetaController {

	private final TargetingMetaService targetingMetaService;

	@Operation(
		summary = "TargetingMeta 처리",
		description = "connectionId와 요청 바디(TargetingMetaRequest)를 기반으로 targeting meta를 처리합니다. (옵션: false)"
	)
	@PostMapping("/targetingmeta/{connectionId}")
	public CommonResponse<ObjectNode> targetingMeta(
			@Parameter(description = "대상 커넥션 ID", required = true, example = "CONN-001")
			@PathVariable String connectionId,
			@Valid @RequestBody TargetingMetaRequest targetingMetaRequest
	) {
		return CommonResponse.success("성공",
				targetingMetaService.targetingMeta(connectionId, targetingMetaRequest, false));
	}

	@Operation(
		summary = "TargetingMeta 처리(옵션 true)",
		description = "connectionId와 요청 바디(TargetingMetaRequest)를 기반으로 targeting meta를 처리합니다. (옵션: true)"
	)
	@PostMapping("/targetingmeta2/{connectionId}")
	public CommonResponse<ObjectNode> targetingMeta2(
			@Parameter(description = "대상 커넥션 ID", required = true, example = "CONN-001")
			@PathVariable String connectionId,
			@Valid @RequestBody TargetingMetaRequest targetingMetaRequest
	) {
		return CommonResponse.success("성공",
				targetingMetaService.targetingMeta(connectionId, targetingMetaRequest, true));
	}
}
