package co.kr.coresolutions.quadengine.query.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Util {

	public static String getBaseUrl(HttpServletRequest request) {
		String scheme = request.getScheme();
		int port = request.getServerPort();
		return scheme + "://" + request.getServerName()
				+ ((("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) ? "" : ":" + port) + request.getContextPath() + "/";
	}

	public static String replaceAll(String strSource, String strSearch, String strReplace) {

		if (strSource == null || strSearch == null || strSearch.equals("")) {
			return strSource;
		}
		if (strReplace == null) {
			strReplace = "";
		}

		int iStart = 0;

		try {
			iStart = strSource.indexOf(strSearch, iStart);

			while (iStart > -1) {
				strSource = strSource.substring(0, iStart) + strReplace + strSource.substring(iStart + strSearch.length());

				iStart += strReplace.length();

				iStart = strSource.indexOf(strSearch, iStart);
			}

		} catch (Exception e) {
			log.error("exception, ", e);
		}

		return strSource;
	}

	public static String getColumnType(String type) {
		String fieldType = "CHAR";

		if (type == null) {
			type = "NULL";
			fieldType = "NULL";
		}

		type = type.toLowerCase();

		if (type.contains("decimal") || type.contains("number")
				|| type.contains("numeric") || type.contains("num")
				|| type.contains("tinyint") || type.contains("tinyint unsigned")
				|| type.contains("smallint unsigned") || type.contains("mediumint")
				|| type.contains("mediumint unsigned") || type.contains("int unsigned")
				|| type.contains("int identity") || type.contains("bigint unsigned")
				|| type.contains("bit") || type.contains("float")
				|| type.contains("int") || type.contains("integer")
				|| type.contains("double") || type.contains("money")) {
			fieldType = "NUM";
		}

		return fieldType;
	}

	public static int getLineNumber() {
		return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}

}
