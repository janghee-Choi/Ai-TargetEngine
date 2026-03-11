package co.kr.coresolutions.quadengine.scheduler.util;

import org.quartz.CronExpression;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateUtils {

	public static final String DEF_DATE_FORMAT = "yyyyMMddHHmmss";

	private static final String SECONDS = "0";
	private static final String DAY_OF_WEEK_FOR_QUARTZ = "?";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DEF_DATE_FORMAT);
	private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private static final Pattern P_DATETIME_PLUS_SECONDS =
			Pattern.compile("^\\s*\\{\\s*DATETIME\\s*\\+\\s*(\\d+)\\s*}\\s*$");

	private static final Pattern P_DATE_PLUS_DAYS_TIME_PLUS_SECONDS =
			Pattern.compile("^\\s*\\{\\s*DATE\\s*\\+\\s*(\\d+)\\s*,\\s*TIME\\s*\\+\\s*(\\d+)\\s*}\\s*$");

	private static final Pattern P_DATE_PLUS_DAYS_AT_TIME =
			Pattern.compile("^\\s*\\{\\s*DATE\\s*\\+\\s*(\\d+)\\s*,\\s*(\\d{1,2}:\\d{2}:\\d{2})\\s*}\\s*$");

	public static String getCronExpOrDate(String schedule, boolean toCron) {
		if (schedule == null || schedule.isBlank()) {
			return "";
		}

		final String input = schedule.trim();

		try {
			ZonedDateTime zdt = null;

			Matcher m1 = P_DATETIME_PLUS_SECONDS.matcher(input);
			if (m1.matches()) {
				long seconds = Long.parseLong(m1.group(1));
				zdt = ZonedDateTime.now(ZoneId.systemDefault()).plusSeconds(seconds);
			} else {
				Matcher m2 = P_DATE_PLUS_DAYS_TIME_PLUS_SECONDS.matcher(input);
				if (m2.matches()) {
					long days = Long.parseLong(m2.group(1));
					long seconds = Long.parseLong(m2.group(2));
					zdt = ZonedDateTime.now(ZoneId.systemDefault()).plusDays(days).plusSeconds(seconds);
				} else {
					Matcher m3 = P_DATE_PLUS_DAYS_AT_TIME.matcher(input);
					if (m3.matches()) {
						long days = Long.parseLong(m3.group(1));
						LocalTime time = LocalTime.parse(m3.group(2));
						LocalDateTime ldt = LocalDateTime.of(LocalDate.now().plusDays(days), time);
						zdt = ldt.atZone(ZoneId.systemDefault());
					}
				}
			}

			if (zdt != null) {
				if (!toCron) {
					return DATE_FORMATTER.format(zdt);
				}
				return toQuartzCronExpression(Date.from(zdt.toInstant()));
			}
		} catch (RuntimeException ex) {
			return "";
		}

		if (input.contains("-")) {
			return "minus";
		}
		return "";
	}

	public static String toQuartzCronExpression(Date date) {
		ZoneId zone = ZoneId.systemDefault();
		Instant instant = date.toInstant();
		ZonedDateTime zdt = instant.atZone(zone);

		int hours = zdt.getHour();
		int minutes = zdt.getMinute();
		int daysOfMonth = zdt.getDayOfMonth();
		int months = zdt.getMonthValue();
		int years = zdt.getYear();

		return SECONDS + " " + minutes + " " + hours + " " + daysOfMonth + " " + months + " " + DAY_OF_WEEK_FOR_QUARTZ + " " + years;
	}

	public static boolean isCronExp(String cronExp) {
		return CronExpression.isValidExpression(cronExp);
	}

	public static boolean isValidDates(String startDT, String endDT) throws ParseException {
		try {
			LocalDateTime start = LocalDateTime.parse(startDT, DATE_FORMATTER);
			LocalDateTime end = LocalDateTime.parse(endDT, DATE_FORMATTER);

			return start.isBefore(end) && end.isAfter(LocalDateTime.now());
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	public static boolean isValidDate(String date) {
		try {
			LocalDate.parse(date, DATE_ONLY_FORMATTER);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	public static boolean isValidDateTime(String date) {
		try {
			LocalDateTime.parse(date, DATE_FORMATTER);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	public boolean isCronExpBetweenRanges(String startDate, String endDate, String cronExp) throws ParseException {
		boolean test = false;
		if (CronExpression.isValidExpression(cronExp)) {
			CronExpression cr = new CronExpression(cronExp);
			Date firstValidFireDate = cr.getNextValidTimeAfter(new SimpleDateFormat(DateUtils.DEF_DATE_FORMAT).parse(startDate));
			Date firstActualValidFireDate = cr.getNextValidTimeAfter(new Date());
			if (firstValidFireDate != null && firstActualValidFireDate != null) {
				if (firstValidFireDate.compareTo(new SimpleDateFormat(DateUtils.DEF_DATE_FORMAT).parse(endDate)) < 0
						&& firstActualValidFireDate.compareTo(new SimpleDateFormat(DateUtils.DEF_DATE_FORMAT).parse(endDate)) < 0) {
					test = true;
				}
			}
		}
		return test;
	}
}


