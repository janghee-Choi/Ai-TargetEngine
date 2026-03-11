package co.kr.coresolutions.quadengine.query.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import co.kr.coresolutions.quadengine.query.util.AES256Cipher;

import java.util.Map;

@Tag(name = "Query", description = "Query 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(QueryRoutes.QUERY_BASE_URL)
public class QueryController {

	@Operation(summary = "상태 확인", description = "서버가 살아있는지 확인합니다.")
	@GetMapping("/status")
	public Map<String, Object> isAlive() {
		return Map.of("status", "alive");
	}

	@Operation(summary = "문자열 암호화", description = "문자열 암호화 수행합니다.")
	@GetMapping("/encrypt")
	public Map<String, Object> encryptString(@RequestParam String input) {
		try {
			return Map.of("string", input, "encrypted", AES256Cipher.AES_Encode(input, true));
		} catch (Exception e) {
			return Map.of("error", "암호화 실패: " + e.getMessage());
		}
	}

}