package co.kr.coresolutions.quadengine.query.controller;

import co.kr.coresolutions.quadengine.common.model.CommonResponse;
import co.kr.coresolutions.quadengine.query.model.PollingRequest;
import co.kr.coresolutions.quadengine.query.service.PollingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(QueryRoutes.QUERY_BASE_URL)
public class PollingController {

	private final PollingService pollingService;

	@PostMapping("/dbpolling")
	public CommonResponse<Void> pooling(@Valid @RequestBody PollingRequest pollingRequest) {
		pollingService.polling(pollingRequest);
		return CommonResponse.success("성공", null);
	}

}
