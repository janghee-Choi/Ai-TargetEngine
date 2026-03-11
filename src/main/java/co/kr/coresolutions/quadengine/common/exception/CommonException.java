package co.kr.coresolutions.quadengine.common.exception;

import lombok.Getter;

@Getter
public class CommonException extends RuntimeException {

	private final CodeInterface codeInterface;
	private String errors = "";

	public CommonException(CodeInterface v) {
		super(v.getMessage());
		this.codeInterface = v;
	}

	public CommonException(CodeInterface v, String errors) {
		super(v.getMessage() + errors);
		this.errors = errors;
		this.codeInterface = v;
	}

}
