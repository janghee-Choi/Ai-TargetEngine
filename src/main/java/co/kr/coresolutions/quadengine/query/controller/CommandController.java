package co.kr.coresolutions.quadengine.query.controller;

import co.kr.coresolutions.quadengine.common.model.CommonResponse;
import co.kr.coresolutions.quadengine.query.model.CommandRequest;
import co.kr.coresolutions.quadengine.query.model.CommandResponse;
import co.kr.coresolutions.quadengine.query.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Command", description = "커맨드 실행/모니터링 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(QueryRoutes.QUERY_BASE_URL)
public class CommandController {

	private final CommandService commandService;

	@Operation(
			summary = "커맨드 실행 여부 확인",
			description = "commandId 기준으로 현재 커맨드가 실행 중인지 확인합니다."
	)
	@GetMapping("/command/isRunning/{commandId}")
	public CommonResponse<Boolean> isRunning(
			@Parameter(description = "확인할 커맨드 ID", required = true, example = "CMD-12345")
			@PathVariable String commandId
	) {
		if (commandService.isRunning(commandId)) {
			return CommonResponse.success("commandId (" + commandId + ") id running", true);
		}
		return CommonResponse.success("commandId (" + commandId + ") is not running", false);
	}

	@Operation(
			summary = "커맨드 모니터링(전체)",
			description = "현재 모니터링 가능한 커맨드 목록을 조회합니다."
	)
	@GetMapping("/command/monitor")
	public List<CommandResponse> monitor() {
		return commandService.monitor();
	}

	@Operation(
			summary = "내 커맨드 조회",
			description = "현재 사용자 기준으로 본인 커맨드 목록을 조회합니다."
	)
	@GetMapping("/command/findMine")
	public CommonResponse<List<CommandResponse>> findMine() {
		return CommonResponse.success("성공", commandService.findMine());
	}

	@Operation(
			summary = "커맨드 등록/실행 요청",
			description = "요청 바디(CommandRequest)로 커맨드를 등록하거나 실행을 요청합니다."
	)
	@PostMapping("/command")
	public String postCommand(@Valid @RequestBody CommandRequest commandRequest) {
		return commandService.postCommand(commandRequest);
	}

	@Operation(
			summary = "커맨드 삭제(모니터링 대상)",
			description = "모니터링 대상 커맨드를 commandId로 삭제합니다."
	)
	@PostMapping("/command/monitor/{commandId}/delete")
	public CommonResponse<Boolean> deleteCommand(
			@Parameter(description = "삭제할 커맨드 ID", required = true, example = "CMD-12345")
			@PathVariable String commandId
	) {
		if (commandService.deleteCommand(commandId)) {
			return CommonResponse.success("commandId {" + commandId + "} deleted successfully", true);
		}
		return CommonResponse.success("commandId {" + commandId + "} doesn't exist", false);
	}

	@Operation(
			summary = "커맨드 로컬 삭제",
			description = "로컬 환경에서 commandId로 커맨드를 삭제합니다."
	)
	@PostMapping("/command/monitorLocal/{commandId}/delete")
	public CommonResponse<Boolean> deleteCommandLocal(
			@Parameter(description = "삭제할 커맨드 ID", required = true, example = "CMD-12345")
			@PathVariable String commandId
	) {
		if (commandService.deleteCommandLocal(commandId)) {
			return CommonResponse.success("commandId {" + commandId + "} deleted successfully", true);
		}
		return CommonResponse.success("commandId {" + commandId + "} doesn't exist", false);
	}

}