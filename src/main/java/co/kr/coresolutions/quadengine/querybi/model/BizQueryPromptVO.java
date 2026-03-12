package co.kr.coresolutions.quadengine.querybi.model;

import lombok.*;
import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BizQueryPromptVO {
	private String queryId;
	private int seq;
	private String promptString;
	private String promptKeyword;
	private String promptOp;

	@Builder.Default
	private boolean removeF = true;

	private String replacePromptValue;
	private String replacePromptOp;
	private String replacePromptType;

	@Builder.Default
	private boolean skip = true;

	private static final String NUMBER_ERR_MSG = "##은 숫자형식만 가능합니다.";

	public String getReplacePromptOp(boolean sqlF) {
		if (!sqlF || replacePromptOp == null)
			return replacePromptOp;

		return switch (replacePromptOp.toLowerCase()) {
		case "equal" -> "=";
		case "less" -> "<";
		case "less_or_equal" -> "<=";
		case "greater" -> ">";
		case "greater_or_equal" -> ">=";
		case "not_in" -> "not in";
		default -> replacePromptOp;
		};
	}

	public boolean isRequired() {
		return promptString != null && promptString.startsWith("[[");
	}

	public String getPromptOpValue() {
		return !skip ? replaceOpValue() : null;
	}

	public String getPromptOpValue(int type) {
		return valueQuote(replaceOpValue(), type);
	}

	private String replaceOpValue() {
		if (replacePromptOp == null || replacePromptValue == null)
			return null;

		String op = replacePromptOp.toLowerCase();

		return switch (op) {
		case "=", "!=", ">", ">=", "<", "<=" -> {
			validateOrThrow(replacePromptValue);
			yield valueQuote(replacePromptValue);
		}
		case "in", "not_in" -> {
			String joined = Arrays.stream(replacePromptValue.split(",")).map(String::trim).peek(this::validateOrThrow)
					.map(this::valueQuote).collect(Collectors.joining(","));
			yield "(" + joined + ")";
		}
		case "between" -> {
			String[] ar = replacePromptValue.split(",");
			if (ar.length < 2)
				yield null;
			validateOrThrow(ar[0].trim());
			validateOrThrow(ar[1].trim());
			yield valueQuote(ar[0]) + " AND " + valueQuote(ar[1]);
		}
		default -> null;
		};
	}

	private String valueQuote(String value) {
		if (value == null)
			return "";
		String trimmed = value.trim();

		if (promptKeyword.contains("@@")) {
			return "'" + trimmed + "'";
		} else if (promptKeyword.contains("##")) {
			return trimmed.replace("'", "");
		} else if (promptKeyword.contains("$$")) {
			return (promptString != null && promptString.contains("'")) ? trimmed.replace("'", "") : trimmed;
		}
		return trimmed;
	}

	private String valueQuote(String value, int type) {
		if (value == null)
			return "";
		String trimmed = value.trim();

		return switch (type) {
		case 2, 3 -> trimmed.replace("'", ""); // ##, $$
		default -> trimmed;
		};
	}

	private void validateOrThrow(String value) {
		if (promptKeyword != null && promptKeyword.contains("##")) {
			if (!value.matches("-?\\d+(\\.\\d+)?")) {
				throw new NumberFormatException(NUMBER_ERR_MSG);
			}
		}
	}
}
