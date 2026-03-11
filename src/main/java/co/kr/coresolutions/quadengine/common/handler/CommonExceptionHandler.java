package co.kr.coresolutions.quadengine.common.handler;

import java.io.PrintWriter;
import java.io.StringWriter;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.common.model.CommonResponse;
import co.kr.coresolutions.quadengine.query.service.QueryService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CommonExceptionHandler {

	private final QueryService queryService;

	@Value("${app.logging-db}")
	private boolean loggingDb;

	@ExceptionHandler(CommonException.class)
	public ResponseEntity<CommonResponse<Void>> handleCommonException(CommonException e) {
		log.error("[ExceptionHandler] 에러 발생");
		log.error(e.getMessage(), e);

		// (260303) 에러 로그 저장
		if (loggingDb) {
			insertErrorLog(e);
		}

		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
				.body(CommonResponse.fail(e));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
		log.error("[ExceptionHandler] 에러 발생");
		log.error(e.getMessage(), e);

		CommonException ce = new CommonException(ErrorCode.FAILED);

		// (260303) 에러 로그 저장
		if (loggingDb) {
			insertErrorLog(ce);
		}

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON)
				.body(CommonResponse.fail(ce));
	}

	public void insertErrorLog(CommonException e) {
		Throwable root = ExceptionUtils.getRootCause(e);
		String stackTrace = "";
		if (root != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			root.printStackTrace(pw);
			stackTrace = sw.toString();
		}

		String insertSql = """
				INSERT INTO
				    T_CMN_ERROR_LOG ( EXCEPTION_TYPE,
									  EXCEPTION_MSG,
									  ORIGIN_CLASS,
									  ORIGIN_METHOD,
									  ORIGIN_LINE,
									  STACK_TRACE,
									  ERROR_DATETIME)
				             VALUES ( :exceptionType,
				                      :exceptionMsg,
				                      :originClass,
				                      :originMethod,
				                      :originLine,
				                      :stackTrace,
				                      CURRENT_TIMESTAMP)
				""";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("exceptionType", e.getClass().getName())
				.addValue("exceptionMsg", e.getMessage()).addValue("originClass", e.getStackTrace()[0].getClassName())
				.addValue("originMethod", e.getStackTrace()[0].getMethodName())
				.addValue("originLine", e.getStackTrace()[0].getLineNumber()).addValue("stackTrace", stackTrace);

		queryService.update(insertSql, params);
	}

}
