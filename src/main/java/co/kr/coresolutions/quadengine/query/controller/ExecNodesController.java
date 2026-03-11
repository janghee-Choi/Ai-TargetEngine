package co.kr.coresolutions.quadengine.query.controller;

import co.kr.coresolutions.quadengine.common.model.CommonResponse;
import co.kr.coresolutions.quadengine.query.model.ExecNodesRequest;
import co.kr.coresolutions.quadengine.query.model.ExecNodesResponse;
import co.kr.coresolutions.quadengine.query.service.ExecNodesService;
import co.kr.coresolutions.quadengine.query.util.Util;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(QueryRoutes.QUERY_BASE_URL)
public class ExecNodesController {

	private final ExecNodesService execNodesService;

	@GetMapping("/execNodes/isRunning/{runId}")
	public CommonResponse<Boolean> isRunning(@PathVariable("runId") String runId) {
		if (execNodesService.isRunning(runId)) {
			return CommonResponse.success("runId (" + runId + ") is running", true);
		}
		return CommonResponse.success("runId (" + runId + ") is not running", false);
	}

	@GetMapping("/execNodes")
	public List<ExecNodesResponse> findAll() {
		return execNodesService.findAll();
	}

	@GetMapping("/execNodes/locally")
	public CommonResponse<List<ExecNodesResponse>> findAllLocally() {
		return CommonResponse.success("성공", execNodesService.findAllLocally());
	}

	@PostMapping("/execNodes/{campId}/{stepId}")
	public CommonResponse<Void> execNodes(
			@PathVariable String campId,
			@PathVariable String stepId,
			@Valid @RequestBody ExecNodesRequest execNodesRequest, HttpServletRequest request) {
		execNodesService.execNodes(campId, stepId, execNodesRequest, Util.getBaseUrl(request));
		return CommonResponse.success("성공", null);
	}

	@PostMapping(value = "/execNodes/{runId}/delete")
	public CommonResponse<Boolean> deleteCommand(@PathVariable String runId) {
		if (execNodesService.stopExecNodes(runId)) {
			return CommonResponse.success("runId {" + runId + "} deleted successfully", true);
		}
		return CommonResponse.success("runId {" + runId + "} doesn't exist", false);
	}

	@PostMapping(value = "/stopExecNodes/local/{runId}/delete")
	public CommonResponse<Boolean> deleteCommandLocal(@PathVariable String runId) {
		if (execNodesService.stopExecNodesLocal(runId)) {
			return CommonResponse.success("runId {" + runId + "} deleted successfully", true);
		}
		return CommonResponse.success("runId {" + runId + "} doesn't exist", false);
	}

}
