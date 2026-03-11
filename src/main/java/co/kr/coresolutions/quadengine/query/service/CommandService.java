package co.kr.coresolutions.quadengine.query.service;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.common.model.CommonResponse;
import co.kr.coresolutions.quadengine.common.util.DateUtils;
import co.kr.coresolutions.quadengine.common.util.HttpUtils;
import co.kr.coresolutions.quadengine.query.executor.RunCommands;
import co.kr.coresolutions.quadengine.query.executor.RunCommandsInternal;
import co.kr.coresolutions.quadengine.query.model.CommandRequest;
import co.kr.coresolutions.quadengine.query.model.CommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandService {

	public static Map<String, CommandResponse> cacheCommand = new ConcurrentHashMap<>();

	private final RunCommands runCommands;
	private final RunCommandsInternal runCommandsInternal;
	private final HttpUtils httpUtils;
	private final Constants constants;

	public boolean isRunning(String commandId) {
		return cacheCommand.containsKey(commandId);
	}

	public List<CommandResponse> monitor() {
		List<CommandResponse> merged = new ArrayList<>();

		if (httpUtils.isRunningModeDuplex()) {
			String url = "/command/findMine";
			CommonResponse<List<CommandResponse>> result = httpUtils.getQueryPartner(url, CommonResponse.class);

			if (result.getSuccess()) {
				List<CommandResponse> partner = Optional.of(result)
						.map(CommonResponse::getData)
						.orElseGet(Collections::emptyList)
						.stream()
						.filter(Objects::nonNull)
						.toList();

				merged.addAll(partner);
			}
		}

		List<CommandResponse> local = cacheCommand.values().stream().filter(Objects::nonNull).toList();
		merged.addAll(local);

		return merged;
	}

	public List<CommandResponse> findMine() {
		return cacheCommand.values().stream().filter(Objects::nonNull).toList();
	}

	public String postCommand(CommandRequest commandRequest) {
		String partMessage = "command triggered, ";
		String message;

		String commandId = commandRequest.getCommandId();
		String commandName = commandRequest.getCommand();
		String owner = commandRequest.getOwner();

		if (commandId == null || commandId.isEmpty()) {
			commandId = "COMMAND_" + UUID.randomUUID();
		} else {
			if (cacheCommand.containsKey(commandId)) {
				message = "Same commandid(" + commandId + ")  is running , so it's canceled";
				log.error("Method : {} , Message : {}", "POST", message);
				throw new CommonException(ErrorCode.COMMAND_ALREADY_EXIST, message);
			}

			if (httpUtils.isRunningModeDuplex()) {
				String url = "/command/isRunning/" + commandId;
				CommonResponse<Boolean> result = httpUtils.getQueryPartner(url, CommonResponse.class);
				if (result.getSuccess() && result.getData()) {
					message = "Same commandid(" + commandId + ")  is running , so it's canceled";
					log.error("Method : {} , Message : {}", "POST", message);
					throw new CommonException(ErrorCode.COMMAND_ALREADY_EXIST, message);
				}
			}
		}

		cacheCommand.put(commandId, CommandResponse.builder()
				.commandId(commandId)
				.commandName(commandName)
				.startDateTime(DateUtils.getNowStr())
				.owner(owner)
				.build());

		try {
			String formattedCommand = getFormattedCommand(commandRequest.getCommand(), commandRequest.getParameter());

			log.info("Method : {} , Message : {}", "POST", Constants.commandDirName + " + BODY  endpoint is started (" + commandRequest + ")\n");
			String resultExecute = executeCommand(commandId, formattedCommand, commandRequest.getRunMode());

			log.info("Method : {} , Message : {}", "POST", Constants.commandDirName + "\noutput message:\n--------------\n\t\t" + (resultExecute.isEmpty() ? "Empty Result" : resultExecute) + "\nBODY:\n--------------\n\t" +
					"\t/" + Constants.commandDirName + "/\n\tPOST\n\t\"" + commandRequest + "\"\n");

			if (resultExecute.isEmpty()) {
				throw new CommonException(ErrorCode.COMMAND_RUN_FAILED, "Empty result returned by the process from Command");
			}

			if (resultExecute.startsWith("Error") || resultExecute.startsWith("Fatal") || resultExecute.startsWith("fail")) {
				throw new CommonException(ErrorCode.COMMAND_RUN_FAILED, resultExecute);
			}

			return resultExecute;
		} finally {
			cacheCommand.remove(commandId);
		}
	}

	public String getFormattedCommand(String commandFileName, ObjectNode parameter) {
		String commandDir = constants.getCommandDir();
		String content = "";
		if (RunCommands.isUnix()) {
			content = new String(constants.getFileContent(Paths.get(commandDir + commandFileName + ".sh")));
		} else if (RunCommands.isWindows()) {
			content = new String(constants.getFileContent(Paths.get(commandDir + commandFileName + ".bat")));
		}

		if (content.isEmpty()) {
			throw new CommonException(ErrorCode.COMMAND_IS_EMPTY);
		}

		if (parameter == null || parameter.isEmpty()) {
			return content;
		}
		String finalCommand = content;
		for (var entry : parameter.properties()) {
			String key = entry.getKey();
			var node = entry.getValue();

			if (!finalCommand.contains(key)) {
				continue;
			}

			String nodeValue;
			if (node == null || node.isNull()) {
				nodeValue = "";
			} else if (node.isArray()) {
				nodeValue = node.toString().replace("\"", "");
			} else {
				nodeValue = node.asText();
			}

			finalCommand = finalCommand.replace(key, nodeValue);
		}
		return finalCommand;
	}

	private String executeCommand(String commandId, String formattedCommand, String runMode) {
		if (formattedCommand != null) {
			String runCommandResult;
			if ("EXTERNAL".equals(runMode)) {
				runCommandResult = runCommands.runCommand(formattedCommand, commandId);
			} else if ("INTERNAL".equals(runMode)) {
				runCommandResult = runCommandsInternal.runCommand(formattedCommand, commandId);
			} else {
				throw new CommonException(ErrorCode.REQUEST_BODY_NOT_VALID, "wrong runMode : " + runMode);
			}

			RunCommands.removeCacheCommandProcess(commandId);
			return runCommandResult;
		}
		return "Error";
	}

	public boolean deleteCommand(String commandId) {
		log.info("POST /" + Constants.commandDirName + " + BODY request is terminated by \"DELETE\" command .(" + cacheCommand.get(commandId) + ")");
		RunCommands.removeCacheCommandProcess(commandId);
		if (deleteCommandLocal(commandId)) {
			return true;
		}

		if (httpUtils.isRunningModeDuplex()) {
			String url = "/command/monitorLocal/" + commandId + "/delete";
			CommonResponse<Boolean> result = httpUtils.postQueryPartner(url, null, CommonResponse.class);
			return result.getSuccess() && result.getData();
		}

		return false;
	}

	public boolean deleteCommandLocal(String commandId) {
		log.info("POST /" + Constants.commandDirName + " + BODY request is terminated by \"DELETE\" command .(" + cacheCommand.get(commandId) + ")");
		RunCommands.removeCacheCommandProcess(commandId);
		CommandResponse commandResponse = cacheCommand.remove(commandId);
		return commandResponse != null;
	}
}
