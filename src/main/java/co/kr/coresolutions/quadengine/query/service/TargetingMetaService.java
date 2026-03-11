package co.kr.coresolutions.quadengine.query.service;

import co.kr.coresolutions.quadengine.common.util.SqlUtils;
import co.kr.coresolutions.quadengine.query.model.TargetingMetaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TargetingMetaService {

	private final QueryService queryService;
	private final ObjectMapper objectMapper;

	public ObjectNode targetingMeta(String connectionId, TargetingMetaRequest targetingMetaRequest, Boolean isJoinKey) {
		String campId = targetingMetaRequest.getCampId();
		String queryType = targetingMetaRequest.getQueryType();
		List<Integer> authList = targetingMetaRequest.getAuthList();
		String campStsId = null;
		List<String> campJoinKeys = null;
		if (SqlUtils.requireNotBlank(campId)) {
			String checkSql = """
					SELECT CAMP_STS_ID
						   %s
					  FROM T_CAMPAIGN
					 WHERE CAMP_ID = :campId
					""".formatted(isJoinKey ? ", JOIN_KEY" : "");
			MapSqlParameterSource params1 = new MapSqlParameterSource()
					.addValue("campId", campId);
			Map<String, Object> resultMap = queryService.selectOne(connectionId, checkSql, params1);
			if (resultMap != null) {
				campStsId = Objects.toString(resultMap.get("CAMP_STS_ID"), null);
				if (isJoinKey) {
					campJoinKeys = List.of(Objects.toString(resultMap.get("JOIN_KEY"), null).split(","));
				}
			}
		}

		StringBuilder whereClause = new StringBuilder();
		if (campJoinKeys != null) {
			whereClause.append(" AND XQL.JOIN_KEY IN (:campJoinKeys))");
		}
		if ("001".equals(campStsId)) {
			whereClause.append(" AND XQL.QRY_ID IN (SELECT XQM.QRY_ID FROM T_XLIG_QUERY_AUTH XQM WHERE XQM.AUTH_ID IN (:authList))");
		}

		String sqlQueryList = """
				SELECT XQL.QRY_ID AS BIZID
					 , XQL.QRY_NM AS BIZNAME
					 , TLV.LOOKUP_CODE AS BIZ_CATEGORY_ID
					 , TLV.LOOKUP_NM AS BIZ_CATEGORY
					 , XQL.QRY_DBMS_ID AS CONNECTIONID
					 , XQL.ADD_GROUP_SHOW_YN
					   %s
				  FROM T_XLIG_QUERY_LIST XQL
				  JOIN T_LOOKUP_VALUE TLV
				    ON TLV.LOOKUP_TYPE_ID = 'QRY_GRP_CD'
				   AND TLV.DEL_F = 'N'
				   AND XQL.QRY_GRP_NM = TLV.LOOKUP_CODE
				 WHERE XQL.QRY_TYPE = :queryType
				   %s
				   AND XQL.DEL_F = 'N'
				   AND XQL.LANG = 'ko_KR'
				 ORDER BY TLV.SORT_SEQ ASC, XQL.QRY_SEQ ASC
				""".formatted(isJoinKey ? ", XQL.JOIN_KEY" : "", whereClause);
		MapSqlParameterSource params2 = new MapSqlParameterSource()
				.addValue("queryType", queryType)
				.addValue("authList", authList)
				.addValue("campJoinKeys", campJoinKeys);
		List<Map<String, Object>> queryList = queryService.selectList(connectionId, sqlQueryList, params2);

		Map<String, List<Object>> groupByQueryId;
		if (queryList.isEmpty()) {
			groupByQueryId = Collections.emptyMap();
		} else {
			List<String> queryIds = queryList.stream().map(map -> Objects.toString(map.get("QRY_ID"), null)).toList();
			String sqlQueryPrompt = """
					SELECT XQP.QRY_ID
						 , XQP.PRMP_JSON_INFO
						 , XQP.SEQ
					  FROM T_XLIG_QUERY_PROMPT XQP
					 WHERE XQP.QRY_ID IN (:queryIds)
					   AND XQP.PRMP_JSON_INFO IS NOT NULL
					 ORDER BY XQP.QRY_ID, XQP.SEQ
					""";
			MapSqlParameterSource params3 = new MapSqlParameterSource()
					.addValue("queryIds", queryIds);
			List<Map<String, Object>> queryPromptList = queryService.selectList(connectionId, sqlQueryPrompt, params3);
			groupByQueryId = queryPromptList.stream()
					.collect(Collectors.groupingBy(
							row -> String.valueOf(row.get("QRY_ID")),
							Collectors.mapping(
									row -> row.get("PRMP_JSON_INFO"),
									Collectors.toList()
							)));
		}

		ObjectNode result = objectMapper.createObjectNode();
		result.putPOJO("BIZQUERY", queryList);
		result.putPOJO("BIZQUERY_FILTERS", groupByQueryId);
		result.putPOJO("Groups", List.of(
				Map.of("GO_NAME", "FD0014", "GO_ID", 1),
				Map.of("GO_NAME", "FD0015", "GO_ID", 2),
				Map.of("GO_NAME", "FD0013", "GO_ID", 3)
		));

		return result;
	}

}

