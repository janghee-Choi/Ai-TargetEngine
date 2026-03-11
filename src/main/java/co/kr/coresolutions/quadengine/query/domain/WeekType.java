package co.kr.coresolutions.quadengine.query.domain;

import java.time.DayOfWeek;

public enum WeekType {

	SUN, MON, TUE, WED, THU, FRI, SAT;

	public static WeekType from(String value) {
		return WeekType.valueOf(value.toUpperCase());
	}

	public static WeekType from(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> MON;
			case TUESDAY -> TUE;
			case WEDNESDAY -> WED;
			case THURSDAY -> THU;
			case FRIDAY -> FRI;
			case SATURDAY -> SAT;
			case SUNDAY -> SUN;
		};
	}
}