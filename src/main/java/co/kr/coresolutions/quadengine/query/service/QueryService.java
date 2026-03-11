package co.kr.coresolutions.quadengine.query.service;

import co.kr.coresolutions.quadengine.query.configuration.ConnectionInfo;
import co.kr.coresolutions.quadengine.query.configuration.DataSourceConfig;
import co.kr.coresolutions.quadengine.query.executor.SqlQueryExecutor;
import co.kr.coresolutions.quadengine.query.model.QueryResultResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

	private final Constants constants;
	private final SqlQueryExecutor sqlQueryExecutor;
	private final DataSourceConfig dataSourceConfig;

	private String _connectionDir;
	private String _rootDir;

	@PostConstruct
	public void init() {
		_connectionDir = constants.getConnectionDir();
		_rootDir = constants.getRootDir();
	}

	public boolean isConnectionIdFileExists(String connectionID) {
		return Files.exists(Paths.get(_connectionDir + connectionID + ".txt"));
	}

	public byte[] getConnectionFile(String connectionID) {
		return constants.getFileContent(Paths.get(_connectionDir + connectionID + ".txt"));
	}

	public ConnectionInfo getConnectionInfo(String connectionID) {
		return dataSourceConfig.getConnectionInfo(connectionID);
	}

	public String getFileFromDir(String fileName, String dir) {
		return new String(constants.getFileContent(Paths.get(_rootDir + dir + File.separator + fileName)));
	}

	public Map<String, Object> selectOne(String query, MapSqlParameterSource params) {
		return sqlQueryExecutor.fetchOne(query, params);
	}

	public Map<String, Object> selectOne(String connectionId, String query, MapSqlParameterSource params) {
		return sqlQueryExecutor.fetchOne(connectionId, query, params);
	}

	public List<Map<String, Object>> selectList(String query, MapSqlParameterSource params) {
		return sqlQueryExecutor.fetchData(query, params);
	}

	public List<Map<String, Object>> selectList(String connectionId, String query, MapSqlParameterSource params) {
		return sqlQueryExecutor.fetchData(connectionId, query, params);
	}

	public QueryResultResponse selectQueryResult(String connectionId, String query, MapSqlParameterSource params) {
		return sqlQueryExecutor.fetchStructuredData(connectionId, query, params);
	}

	public int update(String query, MapSqlParameterSource params) {
		return sqlQueryExecutor.executeUpdate(query, params);
	}

	public int update(String connectionId, String query, MapSqlParameterSource params) {
		return sqlQueryExecutor.executeUpdate(connectionId, query, params);
	}

	public List<Map<String, Object>> loadWithLimits(String connectionId, String query, MapSqlParameterSource params, int limit) {
		return sqlQueryExecutor.fetchDataWithLimit(connectionId, query, params, limit);
	}

	public String checkValidity(String query, String connectionID, String sqliteUrlDB, String queryID, boolean systemPredicates) {
		return "";
	}

	public String checkValidityUpdateClauses(String query, String connectionID) {
		String resultQuery;
		if (!isConnectionIdFileExists(connectionID)) {
			return connectionID;
		}
		try {
			ConnectionInfo connectionInfo = getConnectionInfo(connectionID);
			resultQuery = sqlQueryExecutor.executeQuery(connectionInfo, query);
		} catch (Exception e) {
			log.error("exception, ", e);
			return "error:{" + e.getCause().getMessage() + "}";
		}
		return resultQuery;
	}

}
