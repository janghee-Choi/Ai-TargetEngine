package co.kr.coresolutions.quadengine.scheduler.service;

import co.kr.coresolutions.quadengine.common.util.HttpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallApiService {

	private final HttpUtils httpUtils;

	private static final String CALL_MODE_GET = "GET";
	private static final String CALL_MODE_POST = "POST";

	@Async
	public void asyncCall(String callMode, String callString, JsonNode httpBody) {
		if (callString == null || callString.isEmpty()) {
			return;
		}

		callString = callString.replaceAll("\\\\", "");

		String result = "";
		if (callMode.contentEquals(CALL_MODE_GET)) {
			result = httpUtils.get(callString, String.class);
		} else if (callMode.contentEquals(CALL_MODE_POST)) {
			result = httpUtils.post(callString, httpBody.toString(), String.class);
		}
		log.debug("[API] {} {} = result: {}", callMode, callString, result);
	}

}
