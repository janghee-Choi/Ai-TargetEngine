package co.kr.coresolutions.quadengine.query.executor;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.query.service.Constants;
import co.kr.coresolutions.quadengine.query.service.QueryService;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunCommands {

	@Value("${app.command.shutdown-minutes}")
	private long commandShutdownMinutes;

	private static final String OS = System.getProperty("os.name").toLowerCase();
	public static Map<String, Object> sessionProcess = new ConcurrentHashMap<>();
	public static List<String> commandRandomFlag = new ArrayList<>();

	public static boolean isWindows() {
		return (OS.contains("win"));
	}

	public static boolean isUnix() {
		return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
	}

	private final Constants constants;
	private final QueryService queryService;

	public static Object getCacheCommandProcess(String commandId) {
		return sessionProcess.get(commandId);
	}

	public static void removeCacheCommandProcess(String commandId) {
		Object o = getCacheCommandProcess(commandId);

		if (o != null) {
			if (o instanceof Process) {
				Process process = (Process) o;
				if (process.isAlive()) {
					process.destroyForcibly();
				}
			} else if (o instanceof Thread) {
				Thread thread = (Thread) o;
				if (thread.isAlive()) {
					thread.interrupt();
				}
			} else if (o instanceof CompletableFuture) {
				CompletableFuture completableFuture = (CompletableFuture) o;
				completableFuture.cancel(true);
			}
		}

		sessionProcess.remove(commandId);
	}

	public void saveCacheCommandProcess(String commandId, Object process) {
		sessionProcess.put(commandId, process);
	}

	public String runCommand(String commandAfterKeyReplace, String commandId) {
		final String[] executionResult = { "" };
		final boolean[] commandDurationExceed = { false };
		String randomID = commandId.concat(UUID.randomUUID().toString());
		Thread commandDurationThread = null;
		try {
			Process process;
			ProcessBuilder pb;
			int index = commandAfterKeyReplace.lastIndexOf(";");
			String part1 = commandAfterKeyReplace.substring(0, index);
			String part2 = commandAfterKeyReplace.substring(index);
			String commandAfterKeyReplaceAddCommandId = part1 + ";" + System.lineSeparator() + "COMMAND_ID:" + commandId
					+ part2;
			if (isUnix()) {
				pb = new ProcessBuilder("bash", "-c", commandAfterKeyReplaceAddCommandId);
			} else if (isWindows()) {
				pb = new ProcessBuilder("cmd.exe", "/c", commandAfterKeyReplaceAddCommandId);
			} else {
				pb = new ProcessBuilder(commandAfterKeyReplaceAddCommandId);
			}
			pb.redirectErrorStream(true); // 표준 에러를 표준 출력으로 통합
			process = pb.start();

			saveCacheCommandProcess(commandId, process);
			commandRandomFlag.add(randomID);

			if (commandShutdownMinutes > 0) {
				commandDurationThread = new Thread(() -> {
					try {
						Thread.sleep(commandShutdownMinutes * 60000L);
						if (commandRandomFlag.contains(randomID)) {
							process.destroy();
							commandDurationExceed[0] = true;
							removeCacheCommandProcess(commandId);
							log.info("Method : {} , Message : {}", commandId, "command-execution-time is exceed \n");
							commandRandomFlag.remove(randomID);
						}
					} catch (Exception e) {
						Thread.currentThread().interrupt();
						executionResult[0] = "Error executing command " + commandAfterKeyReplaceAddCommandId
								+ ", Please check manual of your OS!";
						if (commandRandomFlag.contains(randomID)) {
							commandRandomFlag.remove(randomID);
							log.info("Method : {} , Message : {}", "POST",
									"/command is cancelled by request. commandId(" + commandId + ")\n\n");
							throw new CommonException(ErrorCode.COMMAND_CANCELED);
						}
					}
				});
				commandDurationThread.setDaemon(true);
				commandDurationThread.start();
			}

			if (process != null) {
				String input = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
				if (input.isEmpty()) {
					input = "Error Command Execution\t"
							+ IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
				}
				try {
					process.waitFor();
					if (getCacheCommandProcess(commandId) == null) {
						log.info("Method : {} , Message : {}", "POST",
								"Error {" + commandId + "} is aborted by external request\n");
						commandRandomFlag.remove(randomID);
						return "Error {" + commandId + "} is aborted by external request";
					}
				} catch (Exception e) {
					executionResult[0] = "Error executing command " + commandAfterKeyReplaceAddCommandId
							+ ", Please check manual of your OS!";
					log.error("Method : {} , Message : {}", "POST",
							"/command is cancelled by system timeout. commandId(" + commandId + ")\n\n", e);
					Thread.currentThread().interrupt();
					throw new CommonException(ErrorCode.COMMAND_CANCELED);
				} finally {
					commandRandomFlag.remove(randomID);
				}
				return input;
			} else {
				executionResult[0] = "Error executing command in this platform ";
			}

		} catch (Exception e) {
			if (getCacheCommandProcess(commandId) == null) {
				log.error("Method : {} , Message : {}", "POST",
						"Error {" + commandId + "} is aborted by external request", e);
				commandRandomFlag.remove(randomID);
				return "Error {" + commandId + "} is aborted by external request";
			} else {
				if (commandRandomFlag.contains(randomID)) {
					removeCacheCommandProcess(commandId);
					log.error("Method : {} , Message : {}", "POST",
							"Error {" + commandId + "} is aborted by external request", e);
					return "Error {" + commandId + "} is aborted by external request";
				}
			}
		} finally {
			if (commandDurationThread != null && commandDurationThread.isAlive()) {
				commandDurationThread.interrupt();
			}
		}
		commandRandomFlag.remove(randomID);
		return executionResult[0];
	}

}
