package co.kr.coresolutions.quadengine.query.service;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.common.util.SqlUtils;
import co.kr.coresolutions.quadengine.query.model.QueryResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class DimensionService {

	private final QueryService queryService;

	private static final String DIV_CD_DS = "DS";

	public QueryResultResponse queryWithDimension(String connectionId, String langType, String dimensionName, JsonNode replaceNode) {
		String query = """
				SELECT PRMP_KWD_NM, PRMP_KWD_DESC, DBMS_ID, DS_SQL
				  FROM T_XLIG_DIMENSION_LIST
				 WHERE PRMP_KWD = :dimensionName
				   AND ML_DS_DIV_CD = :divCd
				   AND LANG = :langType
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("dimensionName", dimensionName)
				.addValue("divCd", DIV_CD_DS)
				.addValue("langType", langType);
		Map<String, Object> dimension = queryService.selectOne(connectionId, query, params);
		if (dimension == null) {
			throw new CommonException(ErrorCode.DIMENSION_NOT_FOUND, "Dimension not found : " + dimensionName);
		}

		String dbmsId = Objects.toString(dimension.get("DBMS_ID"), null);
		String dsSql = Objects.toString(dimension.get("DS_SQL"), null);
		if (!SqlUtils.requireNotBlank(dbmsId) || !SqlUtils.requireNotBlank(dsSql)) {
			log.debug("Dimension SQL or DBMS ID is missing : {} | {} | {}", dimensionName, dbmsId, dsSql);
			throw new CommonException(ErrorCode.DIMENSION_NOT_FOUND, "Dimension SQL or DBMS ID is missing : " + dimensionName);
		}

		if (replaceNode != null) {
			if(!replaceNode.isEmpty()) {
				for (Map.Entry<String, JsonNode> entry : replaceNode.properties()) {
					String key = entry.getKey();
					String value;
					JsonNode valueNode = entry.getValue();

					if (valueNode == null || valueNode.isNull()) {
						value = "";
					} else if (!valueNode.isValueNode()) {
						throw new CommonException(ErrorCode.DIMENSION_VALUE_TYPE_ERROR, "Unsupported value type for key: {}");
					} else {
						value = valueNode.asText();
					}
					dsSql = dsSql.replace(key, value);
				}
			} else {
				throw new CommonException(ErrorCode.DIMENSION_REPLACENODE_IS_EMPTY, "ReplaceNode is Empty : {}");
			}
		}

		return queryService.selectQueryResult(connectionId, dsSql, null);
	}
}

