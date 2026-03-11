package co.kr.coresolutions.quadengine.query.service;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class Constants {

	private final ObjectMapper objectMapper;

	public static String commandDirName = "command";

	public JsonNode getConfigFileAsJson() {
		try (
				InputStream inputStream = new FileInputStream(System.getProperty("catalina.base") + File.separator + "webapps" + File.separator + "dataqueryConfig" + File.separator + "config.txt");
		) {
			return objectMapper.readValue(inputStream, JsonNode.class);
		} catch (Exception e) {
			return null;
		}
	}

	public String getRootDir() {
		return System.getProperty("catalina.base") + File.separator;
	}

	public String getConnectionDir() {
		log.info("getConnectionDir: {}", System.getProperty("catalina.base") + File.separator + "connection_info" + File.separator);
		return System.getProperty("catalina.base") + File.separator + "connection_info" + File.separator;
	}

	public String getCommandDir() {
		return getRootDir() + commandDirName + File.separator;
	}

	public String getJarsDir() {
		return getRootDir() + "jars" + File.separator;
	}

	public String getDBPollingDir() {
		return getRootDir() + "dbpolling" + File.separator;
	}

	public byte[] getFileContent(Path path) {
		String fileName = path.getFileName().toString();
		if (!Files.exists(path)) {
			throw new CommonException(ErrorCode.FILE_NOT_FOUND, "file not found: " + fileName);
		}

		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path));
			 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			byte[] data = new byte[8192];
			int read;

			while ((read = bis.read(data)) != -1) {
				bos.write(data, 0, read);
			}

			return bos.toByteArray();
		} catch (IOException e) {
			throw new CommonException(ErrorCode.FAILED, e.getMessage());
		}
	}

}
