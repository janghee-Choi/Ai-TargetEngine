package co.kr.coresolutions.quadengine.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

	public static final String DEF_DATE_FORMAT = "yyyyMMddHHmmss";

	public static String getNowStr() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DEF_DATE_FORMAT));
	}

}
