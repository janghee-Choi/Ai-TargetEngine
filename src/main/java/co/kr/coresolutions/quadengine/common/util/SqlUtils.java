package co.kr.coresolutions.quadengine.common.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SqlUtils {

	private static final Set<Integer> NUMERIC_TYPES = Set.of(
			Types.TINYINT,
			Types.SMALLINT,
			Types.INTEGER,
			Types.BIGINT,
			Types.NUMERIC,
			Types.DECIMAL,
			Types.FLOAT,
			Types.REAL,
			Types.DOUBLE
	);

	public static boolean isNumericColumn(ResultSetMetaData metaData, int columnIndex) throws SQLException {
		return NUMERIC_TYPES.contains(metaData.getColumnType(columnIndex));
	}

	public static boolean isNumericType(int sqlType) {
		return NUMERIC_TYPES.contains(sqlType);
	}

	public static boolean requireNotBlank(Object value) {
		String s = Objects.toString(value, null);
		return s != null && !s.isBlank();
	}

	public static BigDecimal getBigDecimal(Map<String, Object> row, String key) {
		return toBigDecimal(row.get(key));
	}

	public static BigDecimal toBigDecimal(Object value) {
		return switch (value) {
			case BigDecimal bd -> bd;
			case BigInteger bi -> new BigDecimal(bi);
			case Number n -> BigDecimal.valueOf(n.doubleValue());
			case null, default -> null;
		};

	}

	/**
	 * Map 목록을 지정된 DTO 클래스로 변환합니다.
	 * @param <T> 변환할 대상 타입
	 * @param mapList 원본 Map 목록
	 * @param targetClass 변환할 대상 클래스
	 * @return 변환된 객체 목록
	 */
	public static <T> List<T> mapToList(List<Map<String, Object>> mapList, Class<T> targetClass) {
		ObjectMapper mapper = new ObjectMapper();
		return mapList.stream()
				.map(map -> mapper.convertValue(map, targetClass))
				.toList();
	}

}
