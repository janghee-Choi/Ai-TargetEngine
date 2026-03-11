package co.kr.coresolutions.quadengine.common.model;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonResponse<T> {

	private final Boolean success;
	private final Integer code;
	private final String message;
	private final String errors;
	private final T data;

	public static <T> CommonResponse<T> success(String message, T data) {
		return new CommonResponse<>(true, null, message, "", data);
	}

	public static <T> CommonResponse<T> fail(CommonException e) {
		Integer code = e.getCodeInterface().getCode();
		String message = e.getCodeInterface().getMessage();
		String errors = e.getErrors();
		return new CommonResponse<>(false, code, message, errors == null ? "" : errors, null);
	}
}