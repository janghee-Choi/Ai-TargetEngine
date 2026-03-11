package co.kr.coresolutions.quadengine.query.model.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DateValidation {

	private Month month;

	private Week week;

	private Boolean validateHoliday;

	@Data
	public static class Month {
		private String jan;
		private String feb;
		private String mar;
		private String apr;
		private String may;
		private String jun;
		private String jul;
		private String aug;
		private String sep;
		private String oct;
		private String nov;
		private String dec;
	}

	@Data
	public static class Week {
		private String sun;
		private String mon;
		private String tue;
		private String wed;
		private String thu;
		private String fri;
		private String sat;
	}

}