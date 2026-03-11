package co.kr.coresolutions.quadengine.query.domain;

import java.time.Month;

public enum MonthType {

	JAN, FEB, MAR, APR, MAY, JUN,
	JUL, AUG, SEP, OCT, NOV, DEC;

	public static MonthType from(String value) {
		return MonthType.valueOf(value.toUpperCase());
	}

	public static MonthType from(Month month) {
		return MonthType.values()[month.getValue() - 1];
	}
}
