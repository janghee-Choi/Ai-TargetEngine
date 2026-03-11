package co.kr.coresolutions.quadengine.query.executor;

import co.kr.coresolutions.quadengine.query.configuration.ConnectionInfo;
import co.kr.coresolutions.quadengine.query.configuration.DataSourceConfig;
import co.kr.coresolutions.quadengine.query.dao.AbstractDao;
import co.kr.coresolutions.quadengine.query.model.QueryResultResponse;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static co.kr.coresolutions.quadengine.common.util.SqlUtils.isNumericColumn;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SqlQueryExecutor extends AbstractDao {

	private final DataSourceConfig dataSourceConfig;
	private final NamedParameterJdbcTemplate jdbcTemplate;

	public int getRowCount(String connectionId, String query, MapSqlParameterSource params) {
		NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSourceConfig.getDataSource(connectionId));
		String countQuery = wrapCountQuery(query);
		Integer count = jdbcTemplate.queryForObject(countQuery, params, Integer.class);
		return count != null ? count : 0;
	}

	private String wrapCountQuery(String query) {
		return "SELECT COUNT(1) CNT FROM (" + query + ") T";
	}

	public Map<String, Object> fetchOne(String query, MapSqlParameterSource params) {
		try {
			return jdbcTemplate.queryForMap(query, params);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Map<String, Object> fetchOne(String connectionId, String query, MapSqlParameterSource params) {
		try {
			NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSourceConfig.getDataSource(connectionId));
			return jdbcTemplate.queryForMap(query, params);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<Map<String, Object>> fetchData(String query, MapSqlParameterSource params) {
		return jdbcTemplate.queryForList(query, params);
	}

	public List<Map<String, Object>> fetchData(String connectionId, String query, MapSqlParameterSource params) {
		NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSourceConfig.getDataSource(connectionId));
		return jdbcTemplate.queryForList(query, params);
	}

	public List<Map<String, Object>> fetchDataWithLimit(String connectionId, String query, MapSqlParameterSource params, int limit) {
		NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSourceConfig.getDataSource(connectionId));
		jdbcTemplate.getJdbcTemplate().setMaxRows(limit);
		return jdbcTemplate.queryForList(query, params);
	}

	public QueryResultResponse fetchStructuredData(String connectionId, String query, MapSqlParameterSource params) {
		NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSourceConfig.getDataSource(connectionId));
		return jdbcTemplate.query(query, params, rs -> {
			ResultSetMetaData meta = rs.getMetaData();
			int colCount = meta.getColumnCount();

			List<QueryResultResponse.Field> fields = new ArrayList<>(colCount);
			for (int i = 1; i <= colCount; i++) {
				QueryResultResponse.Field f = new QueryResultResponse.Field(
						meta.getColumnLabel(i),
						meta.getColumnLabel(i),
						meta.getColumnTypeName(i)
				);
				fields.add(f);
			}

			List<Map<String, Object>> rows = new ArrayList<>();
			while (rs.next()) {
				Map<String, Object> row = new LinkedHashMap<>(colCount);
				for (int i = 1; i <= colCount; i++) {
					Object value = rs.getObject(i);

					if (value != null && isNumericColumn(meta, i)) {
						String s = String.valueOf(value);
						if (isNumber(s)) {
							value = new BigDecimal(s).toBigInteger();
						}
					}

					row.put(meta.getColumnLabel(i), value);
				}
				rows.add(row);
			}

			return new QueryResultResponse(rows, fields);
		});
	}

	public int executeUpdate(String query, MapSqlParameterSource params) {
		return jdbcTemplate.update(query, params);
	}

	public int executeUpdate(String connectionId, String query, MapSqlParameterSource params) {
		NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSourceConfig.getDataSource(connectionId));
		return jdbcTemplate.update(query, params);
	}

	public String executeQuery(ConnectionInfo connectionInfo, String formattedString) {
		int numberOfRows = 0;
		SessionFactory _tempSessionFactory = null;
		Session session = null;
		@SuppressWarnings("rawtypes")
		NativeQuery nQuery;
		try {
			_tempSessionFactory = createSessionFactory(connectionInfo);
			session = _tempSessionFactory.openSession();
			Transaction tx = session.beginTransaction();
			nQuery = session.createNativeQuery(formattedString);
			numberOfRows = nQuery.executeUpdate();

			tx.commit();
			session.clear();
			session.close();
			session = null;
			_tempSessionFactory.close();
		} catch (Exception ex) {
			log.error("Method : {} , Message : {}", "Query Update", "SQL Query is\t" + formattedString + " failed " + ex.getMessage() + "\n", ex);
			if (ex.getCause() != null) {
				log.error("Method : {} , Message : {}", "Query Update", "SQL Query is\t" + formattedString + "\n", ex);
				return ("Error : {" + ex.getCause().getMessage() + "}");
			}
		} finally {
			if (session != null) {
				session.clear();
				session.close();
			}
			if (_tempSessionFactory != null) {
				_tempSessionFactory.close();
			}
		}
		return "success count : " + numberOfRows;
	}

	public String executeQuerySelectJDBC(ConnectionInfo connectionInfo, String connectionID, String sql, String queryid, boolean isLabel, boolean isColumnType, long liquidLimit) {
		return "";
	}

	public String executeQuerySelectJDBCLimits(ConnectionInfo connectionInfo, String connectionID, String sql, String queryid, boolean isLabel, int limits) {
		return "";
	}

	public boolean isNumber(String str) {
		return NumberUtils.isCreatable(str);
	}

	public Connection getConnection(String connId) throws SQLException {
		ConnectionInfo connectionInfo = dataSourceConfig.getConnectionInfo(connId);
		HikariDataSource dataSource = dataSourceConfig.getDataSource(connId, connectionInfo);
		return dataSource.getConnection();
	}
}

