package co.kr.coresolutions.quadengine.query.controller;

import co.kr.coresolutions.quadengine.common.model.CommonResponse;
import co.kr.coresolutions.quadengine.query.model.NodeWorkRequest;
import co.kr.coresolutions.quadengine.query.model.NodeWorkDeleteRequest;
import co.kr.coresolutions.quadengine.query.service.NodeWorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "NodeWork", description = "노드 작업(카운트/삭제) 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(QueryRoutes.QUERY_BASE_URL)
public class NodeWorkController {

	private final NodeWorkService nodeWorkService;

	@Operation(
		summary = "노드 카운트 조회",
		description = "connectionId와 요청 바디(NodeWorkRequest)를 기반으로 노드 카운트를 조회합니다."
	)
	@PostMapping("/{connectionId}/count")
	public CommonResponse<Map<String, Object>> postCount(
			@Parameter(description = "대상 커넥션 ID", required = true, example = "CONN-001")
			@PathVariable String connectionId,
			@Valid @RequestBody NodeWorkRequest nodeWorkRequest
	) {
		return CommonResponse.success("조회 성공", nodeWorkService.countNodes(connectionId, nodeWorkRequest));
	}

	@Operation(
		summary = "노드(테이블) 삭제",
		description = "요청 바디(NodeWorkDeleteRequest)에 포함된 정보로 테이블/노드 삭제를 수행합니다."
	)
	@PostMapping("/node/delete")
	public CommonResponse<Void> nodeDelete(@RequestBody NodeWorkDeleteRequest nodeWorkDeleteRequest) {
		nodeWorkService.dropTable(nodeWorkDeleteRequest);
		return CommonResponse.success("삭제 성공", null);
	}

}
