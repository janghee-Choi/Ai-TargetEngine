package co.kr.coresolutions.quadengine.query.service;

import co.kr.coresolutions.quadengine.query.configuration.ConnectionInfo;
import co.kr.coresolutions.quadengine.query.model.NodeWorkRequest;
import co.kr.coresolutions.quadengine.query.model.NodeWorkDeleteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeWorkService {

	private final QueryService queryService;

	public Map<String, Object> countNodes(String connectionId, NodeWorkRequest nodeWorkRequest) {
		String query = """
				SELECT DISTINCT NODE_ID
					 , TOT_EXTR_CNT AS CNT
				  FROM T_CAMP_NODE_EXEC_RESULT
				 WHERE CAMP_ID = :campId
				   AND NODE_ID IN (:nodeIds)
				""";
		List<String> nodeIds = nodeWorkRequest.getNodes().properties().stream().map(Map.Entry::getKey).toList();
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("campId", nodeWorkRequest.getCampId())
				.addValue("nodeIds", nodeIds);
		List<Map<String, Object>> resultList = queryService.selectList(connectionId, query, params);
		Map<String, String> resultMap = resultList.stream()
				.collect(Collectors.toMap(
						row -> Objects.toString(row.get("NODE_ID"), null),
						row -> Objects.toString(row.get("CNT"), null)
				));

		return nodeIds.stream()
				.collect(Collectors.toMap(
						nodeId -> nodeId,
						nodeId -> resultMap.getOrDefault(nodeId, "error")
				));
	}

	public void dropTable(NodeWorkDeleteRequest nodeWorkDeleteRequest) {
		String connectionId = nodeWorkDeleteRequest.getConnId();
		String workConnectionId = nodeWorkDeleteRequest.getWorkConnId();
		String workTableName = nodeWorkDeleteRequest.getTableName();

		//TODO
		ConnectionInfo workConnectionInfo = queryService.getConnectionInfo(workConnectionId);
		if (workConnectionInfo.getDbms().equalsIgnoreCase("ORACLE")) {

		} else if (workConnectionInfo.getDbms().equalsIgnoreCase("MARIADB") || workConnectionInfo.getDbms().equalsIgnoreCase("MYSQL")) {

		} else {

		}
//		String dropResultValidity;
//		if (workDbms.equalsIgnoreCase("ORACLE")) {
//			dropResultValidity = queryService.checkValidityUpdateClauses("BEGIN EXECUTE IMMEDIATE 'DROP TABLE " + workTableName + "'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;", workConnectionId);
//		} else if (workDbms.equalsIgnoreCase("MARIADB") || workDbms.equalsIgnoreCase("MYSQL")) {
//			dropResultValidity = queryService.checkValidityUpdateClauses("DROP TABLE IF EXISTS " + workTableName, workConnectionId);
//		} else {
//			dropResultValidity = queryService.checkValidityUpdateClauses("DROP TABLE " + workTableName, workConnectionId);
//		}

		// T_CAMP_NODE_EXEC_RESULT 삭제
		String deleteSql = """
				DELETE FROM T_CAMP_NODE_EXEC_RESULT
				 WHERE CAMP_ID = :campId
				   AND NODE_ID = :nodeId
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("campId", nodeWorkDeleteRequest.getCampId())
				.addValue("nodeId", nodeWorkDeleteRequest.getNodeId());
		int deleteResult = queryService.update(connectionId, deleteSql, params);
	}
}

