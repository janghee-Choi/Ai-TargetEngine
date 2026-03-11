package co.kr.coresolutions.quadengine.scheduler.controller;


import co.kr.coresolutions.quadengine.scheduler.domain.Schedule;
import co.kr.coresolutions.quadengine.scheduler.model.ScheduleRequest;
import co.kr.coresolutions.quadengine.scheduler.service.SchedulerService;
import co.kr.coresolutions.quadengine.scheduler.validation.ScheduleRequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Schedule", description = "Quartz 스케줄 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(SchedulerRoutes.SCHEDULER_BASE_URL + "/schedule")
public class ScheduleController {

	private final SchedulerService schedulerService;
	private final ScheduleRequestValidator scheduleRequestValidator;

	@Operation(
			summary = "서버 상태 확인",
			description = "클라이언트 IP 및 서버 포트를 반환합니다. (헬스체크/네트워크 확인용)"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "정상 응답",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(
									implementation = Map.class,
									example = "{\"IPAddress\":\"127.0.0.1\",\"Port\":8080}"
							)
					)
			)
	})
	@GetMapping("/isAlive")
	public Map<String, Object> getInformationAboutServer(HttpServletRequest request) {
		Map<String, Object> map = new HashMap<>();
		map.put("IPAddress", request.getRemoteAddr());
		map.put("Port", request.getServerPort());
		return map;
	}

	@Operation(
			summary = "활성 스케줄 목록 조회",
			description = "현재 활성 상태의 스케줄 목록을 반환합니다."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "정상 응답",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							array = @ArraySchema(schema = @Schema(implementation = Schedule.class))
					)
			)
	})
	@GetMapping
	public List<Schedule> listActiveSchedule() {
		return schedulerService.activeSchedules();
	}

	@Operation(
			summary = "스케줄 단건 조회",
			description = "스케줄 ID(scId)로 스케줄 정보를 조회합니다."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "정상 응답",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Schedule.class))
			),
			@ApiResponse(responseCode = "404", description = "대상 스케줄을 찾을 수 없음", content = @Content),
			@ApiResponse(responseCode = "400", description = "요청 파라미터 오류", content = @Content)
	})
	@GetMapping(value = "/{scId}")
	public Schedule getSchedule(
			@Parameter(description = "스케줄 ID", example = "job-001", required = true)
			@PathVariable String scId
	) {
		return schedulerService.getSchedule(scId);
	}

	@Operation(
			summary = "스케줄 등록",
			description = "스케줄 ID(scId)와 요청 본문을 기반으로 스케줄을 등록합니다."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "등록 성공",
					content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(example = "Success, 200"))
			),
			@ApiResponse(responseCode = "400", description = "요청 본문/형식 오류", content = @Content),
			@ApiResponse(responseCode = "409", description = "중복/충돌(이미 존재하는 스케줄 등)", content = @Content),
			@ApiResponse(responseCode = "500", description = "스케줄러 내부 오류", content = @Content)
	})
	@PostMapping(value = "/{scId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> addSchedule(
			@Parameter(description = "스케줄 ID", example = "job-001", required = true)
			@PathVariable String scId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					description = "스케줄 등록 요청",
					content = @Content(schema = @Schema(implementation = ScheduleRequest.class))
			)
			@RequestBody ScheduleRequest scheduleRequest
	) throws SchedulerException, ParseException {
		scheduleRequestValidator.validate(scheduleRequest);

		schedulerService.addSchedule(scId, scheduleRequest);
		return ResponseEntity.ok("Success, 200");
	}

	@Operation(
			summary = "스케줄 수정",
			description = "기존 스케줄을 수정합니다. 스케줄 ID(scId)를 기준으로 업데이트합니다."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "수정 성공",
					content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(example = "Success, 200"))
			),
			@ApiResponse(responseCode = "400", description = "요청 본문/형식 오류", content = @Content),
			@ApiResponse(responseCode = "404", description = "대상 스케줄을 찾을 수 없음", content = @Content),
			@ApiResponse(responseCode = "500", description = "스케줄러 내부 오류", content = @Content)
	})
	@PostMapping(value = "/update/{scId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> updateSchedule(
			@Parameter(description = "스케줄 ID", example = "job-001", required = true)
			@PathVariable String scId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					description = "스케줄 수정 요청",
					content = @Content(schema = @Schema(implementation = ScheduleRequest.class))
			)
			@RequestBody ScheduleRequest scheduleRequest
	) throws SchedulerException, ParseException {
		scheduleRequestValidator.validate(scheduleRequest);

		schedulerService.updateSchedule(scId, scheduleRequest);
		return ResponseEntity.ok("Success, 200");
	}

	@Operation(
			summary = "스케줄 삭제",
			description = "스케줄 ID(scId)에 해당하는 스케줄을 삭제합니다."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "삭제 성공",
					content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(example = "Success, 200"))
			),
			@ApiResponse(responseCode = "404", description = "대상 스케줄을 찾을 수 없음", content = @Content),
			@ApiResponse(responseCode = "500", description = "스케줄러 내부 오류", content = @Content)
	})
	@PostMapping(value = "/delete/{scId}", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> deleteSchedule(
			@Parameter(description = "스케줄 ID", example = "job-001", required = true)
			@PathVariable String scId
	) throws SchedulerException {
		schedulerService.deleteSchedule(scId);
		return ResponseEntity.ok("Success, 200");
	}
}
