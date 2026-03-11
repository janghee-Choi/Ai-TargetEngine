package co.kr.coresolutions.quadengine.query.configuration;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.query.service.Constants;
import co.kr.coresolutions.quadengine.query.util.AES256Cipher;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceConfig {

	private final Map<String, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>(); // db별 pool
	private final Map<String, ConnectionInfo> connectionMap = new ConcurrentHashMap<>(); // config
	private final Map<String, Long> lastUsedTimeMap = new ConcurrentHashMap<>(); //
	private final Constants constants;
	private final ObjectMapper objectMapper;
	private final QueryDataSourceProperties properties;

	private Path connectionDir;

	@Value("${app.encrypt.password}")
	private Boolean encryptPassword;

	// Spring 초기화 시점에 모든 연결 파일을 로드 & DB Connection Pool Set & 히카리풀, dataSourceMap ,
	// close 스케줄.
	@PostConstruct
	public void init() {
		this.connectionDir = Paths.get(constants.getConnectionDir());
		// 파일로드 및 connectionMapdp put
		loadAllConnectionFiles();
		if (!connectionMap.isEmpty()) {
			connectionMap.keySet().forEach(this::initializeDataSource);
		}
	}

	@PreDestroy
	public void shutdown() {
		// 애플리케이션 종료 시 풀 정리
		dataSourceMap.forEach((id, ds) -> {
			try {
				log.info("Closing datasource: {}", id);
				ds.close();
			} catch (Exception e) {
				log.warn("Failed to close datasource: {}", id, e);
			}
		});
		dataSourceMap.clear();
	}

	private boolean isNonDatabaseConnection(ConnectionInfo connectionInfo) {
		if (connectionInfo == null || connectionInfo.getDbms() == null) {
			return false;
		}
		return connectionInfo.getDriverClassName() == null || connectionInfo.getDriverClassName().isEmpty();
	}

	private void loadAllConnectionFiles() {
		if (!Files.exists(connectionDir)) {
			log.warn("Connection directory does not exist: {}", connectionDir);
			return;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(connectionDir, "*.txt")) {
			for (Path path : stream) {
				String fileName = path.getFileName().toString();
				String connectionID = fileName.substring(0, fileName.lastIndexOf('.'));

				try {
					byte[] content = constants.getFileContent(path);
					ConnectionInfo connectionInfo = objectMapper.readValue(content, ConnectionInfo.class);
					setConnection(connectionInfo);

					if (!connectionInfo.getPassword().isEmpty()) {
						connectionMap.put(connectionID, connectionInfo);
						log.info("Loaded connection file: {}", connectionID);
					} else {
						log.info("Connection fail due to password problem: {}", connectionID);
					}
				} catch (UnrecognizedPropertyException e) {
					// kafka등 제외되는 애들 찍히는 로그.
					log.info("Skipping connection file (not a database connection): {}", fileName);
				} catch (Exception e) {
					log.error("Connection file could not be loaded or is empty: {} - {}", fileName, e.getMessage());
				}
			}
		} catch (IOException e) {
			log.error("Failed to load connection: returning empty connection", e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			log.error("Failed to load connection files: returning empty connection", e);
			throw new RuntimeException(e);
		}
		log.info("Loaded {} connection files", connectionMap.size());
	}

	public HikariDataSource createDataSource(String connId, ConnectionInfo connectionInfo) {
		try {
			log.info("Creating new HikariDataSource for connection id {}", connId);

			if (connectionInfo == null) {
				throw new IllegalStateException("Connection configuration not found: " + connId);
			}

			HikariConfig config = getHikariConfig(connectionInfo);
			HikariDataSource dataSource = new HikariDataSource(config);

			log.info("Successfully created HikariDataSource for connId: {}", connId);

			return dataSource;
		} catch (Exception e) {
			log.error("Failed to create HikariDataSource for connId: {}", connId, e);
			throw new RuntimeException("Failed to create HikariDataSource for connId: " + connId, e);
		}
	}

	private void setDataSourceReadTimeOut(HikariConfig config) {
		String className = config.getDriverClassName();

		switch (className) {
		case "oracle.jdbc.OracleDriver":
		case "oracle.jdbc.driver.OracleDriver":
			config.addDataSourceProperty("oracle.net.READ_TIMEOUT", "1800000");
			break;
		case "org.postgresql.Driver":
			config.addDataSourceProperty("socketTimeout", "1800");
			break;
		case "com.mysql.cj.jdbc.Driver":
		case "com.mysql.jdbc.Driver":
		case "org.mariadb.jdbc.Driver":
			config.addDataSourceProperty("socketTimeout", "1800000");
			break;
		case "com.microsoft.sqlserver.jdbc.SQLServerDriver":
			config.addDataSourceProperty("socketTimeout", "1800000");
			break;
		default:
			config.addDataSourceProperty("socketTimeout", "1800000");
		}
	}

	private HikariConfig getHikariConfig(ConnectionInfo connectionInfo) {
		// DRIVER가 null이거나 비어있는 경우 예외 처리
		if (connectionInfo.getDriverClassName() == null || connectionInfo.getDriverClassName().trim().isEmpty()) {
			throw new IllegalArgumentException("DRIVER is required for database connection");
		}

		HikariConfig config = new HikariConfig();
		config.setDriverClassName(connectionInfo.getDriverClassName());
		String url = connectionInfo.getUrl();

		config.setMaximumPoolSize(properties.getMaximumPoolSize());
		config.setMinimumIdle(properties.getMinimumIdle());
		// 해당 설정 부분 타임아웃등 어디에 넣고 관리할지 정한후 작업 필요.
		config.setUsername(connectionInfo.getUsername());
		config.setPassword(connectionInfo.getPassword());
		config.setConnectionTimeout(properties.getConnectionTimeout()); // 커넥션 대기 시간 (30초)
		config.setIdleTimeout(properties.getIdleTimeout()); // 10분 idle
		config.setMaxLifetime(properties.getMaxLifetime()); // 30분 lifetime
		config.setLeakDetectionThreshold(properties.getLeakDetectionThreshold()); // 커넥션 누수 감지 (1분)

		// readtimeout 확인후 추가.
		config.setJdbcUrl(url);

		setDataSourceReadTimeOut(config);

		String optQuery = connectionInfo.getOptionPrefix();
		if (optQuery != null && !optQuery.trim().isEmpty()) {
			config.setConnectionInitSql(optQuery);
		}

		return config;
	}

	public synchronized HikariDataSource getDataSource(String connId) {
		if (dataSourceMap.containsKey(connId)) {
			return dataSourceMap.get(connId);
		}
		throw new CommonException(ErrorCode.CONNECTION_NOT_FOUND, "Not found connection id : " + connId);
	}

	public synchronized HikariDataSource getDataSource(String connId, ConnectionInfo connectionInfo) {
		lastUsedTimeMap.put(connId, System.currentTimeMillis());
		if (isNonDatabaseConnection(connectionInfo)) {
			log.info("Skipping connection file (not a database connection) DBMS : {}", connectionInfo.getDbms());
			return null;
		}
		HikariDataSource dataSource = dataSourceMap.computeIfAbsent(connId, id -> createDataSource(id, connectionInfo));
		HikariPoolMXBean poolMXBean = dataSource.getHikariPoolMXBean();
		log.info("Loaded connection ID : {}, Active Connections : {}, Max Pool Size : {}", connId,
				poolMXBean.getActiveConnections(), dataSource.getMaximumPoolSize());
		return dataSource;
	}

	private ConnectionInfo getPWConnectionFromJar(String pwConnectionId) {
		try {
			String path = constants.getJarsDir() + pwConnectionId + ".jar";
			File file = Paths.get(path).toFile();
			URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { file.toURI().toURL() },
					this.getClass().getClassLoader());
			Class classToLoad = Class.forName("co.kr.coresolutions.jars.Main", true, urlClassLoader);
			Method method = classToLoad.getDeclaredMethod("getConnection");
			Object instance = classToLoad.newInstance();
			return objectMapper.readValue(method.invoke(instance).toString(), ConnectionInfo.class);
		} catch (Exception e) {
			return ConnectionInfo.builder().build();
		}
	}

	public ConnectionInfo getConnectionInfo(String connectionID) {
		return connectionMap.computeIfAbsent(connectionID, this::loadAndCacheConnection);
	}

	private ConnectionInfo loadAndCacheConnection(String connectionID) {
		try {
			// 윈도우 환경에선 파일 읽을때, 대소문자 구분을 안해서 소문자로 된 파일만 있는 상태에서, connectionID가 대문자로 들어와도
			// 대문자로 map 하나 더 생성됨.
			// 리눅스는 구별하므로 큰 이상은 없어보임.
			Path path = connectionDir.resolve(connectionID + ".txt");
			ConnectionInfo connectionInfo = objectMapper.readValue(constants.getFileContent(path),
					ConnectionInfo.class);
			setConnection(connectionInfo);
			return connectionInfo;
		} catch (Exception e) {
			log.error("Failed to load connection: {}, returning empty connection", connectionID);
			return null;
		}
	}

	@SneakyThrows
	private void setConnection(ConnectionInfo connectionInfo) {
		if (connectionInfo == null) {
			return; // 또는 기본 Connection 객체 생성
		}

		if (connectionInfo.getPasswordConnection() != null) {
			connectionInfo = getPWConnectionFromJar(connectionInfo.getPasswordConnection());
		}
		if (connectionInfo.getSchema() == null) {
			connectionInfo.setSchema("");
		}
		if (connectionInfo.getPassword() == null) {
			connectionInfo.setPassword("");
		} else {
			connectionInfo.setPassword(AES256Cipher.AES_Decode(connectionInfo.getPassword(), encryptPassword));
		}
		if (connectionInfo.getUsername() == null) {
			connectionInfo.setUsername("");
		}
		if (connectionInfo.getHttpUrl() == null) {
			connectionInfo.setHttpUrl("");
		}
		if (connectionInfo.getDriverClassName() == null) {
			connectionInfo.setDriverClassName("");
		}
		if (connectionInfo.getUrl() == null) {
			connectionInfo.setUrl("");
		}
		if (connectionInfo.getDbms() == null) {
			connectionInfo.setDbms("");
		}
		if (connectionInfo.getHost() == null) {
			connectionInfo.setHost("");
		}
		if (connectionInfo.getPort() == null) {
			connectionInfo.setPort("");
		}
		if (connectionInfo.getConnName() == null) {
			connectionInfo.setConnName("");
		}
		if (connectionInfo.getEtc() == null) {
			connectionInfo.setEtc("");
		}
		if (connectionInfo.getOwner() == null) {
			connectionInfo.setOwner("");
		}
	}

	private void initializeDataSource(String key) {
		getDataSource(key, connectionMap.get(key));
	}

}
