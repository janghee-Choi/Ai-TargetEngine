package co.kr.coresolutions.quadengine.query.executor;

import co.kr.coresolutions.quadengine.common.event.ScheduleEvent;
import co.kr.coresolutions.quadengine.common.event.SchedulePublisher;
import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.query.model.CommandRequest;
import co.kr.coresolutions.quadengine.query.model.dtos.NodeCommand;
import co.kr.coresolutions.quadengine.query.service.CommandService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecNodes {

	private static Map<String, String> cacheNode = new ConcurrentHashMap<>();

	private final CommandService commandService;
	private final SchedulePublisher schedulePublisher;
	private final ObjectMapper objectMapper;

	public void run(String runId, Long seq, String source, List<NodeCommand> commandList) {
		final String finalRunId = runId;

		try {
			boolean anyMatchIssueExecution = commandList.stream()
					.anyMatch(nodeCommand -> {
						String url = nodeCommand.getUrl().replaceAll("@@SEQ@@", String.valueOf(seq + 1));
						JsonNode jsonBody = nodeCommand.getBatchBody();

						if (source != null && source.equals("body")) {
							jsonBody = nodeCommand.getBody();
						}

						if (jsonBody.isNull()) {
							return false;
						}

						if (!jsonBody.isArray()) {
							return true;
						}

						return StreamSupport.stream(jsonBody.spliterator(), false)
								.anyMatch(jsonNode -> {
									try {
										String body = jsonNode.toString()
												.replaceAll("@@SEQ@@", String.valueOf(seq + 1))
												.replaceAll("@@SEQ0@@", String.valueOf(seq));
										if (url.endsWith("command")) {
											CommandRequest commandRequest = objectMapper.readValue(body, CommandRequest.class);
											cacheNode.put(finalRunId, Optional.ofNullable(cacheNode.get(finalRunId)).orElse("").concat(commandRequest.getCommandId()).concat("\n"));
											commandService.postCommand(commandRequest);
										} else {
											ScheduleEvent scheduleEvent = objectMapper.readValue(body, ScheduleEvent.class);
											schedulePublisher.sendScheduleEvent(scheduleEvent);
										}
										return true;
									} catch (Exception e) {
										log.error("ExecNodes Error Message : ", e);
										return false;
									}
								});
					});

			if (anyMatchIssueExecution) {
				throw new CommonException(ErrorCode.EXEC_NODE_RUN_FAILED);
			}
		} finally {
			cacheNode.remove(runId);
		}
	}

	public void stop(String runId) {
		String commandId = cacheNode.get(runId);
		if (!StringUtils.isEmpty(commandId)) {
			commandService.deleteCommand(commandId);
		}
	}
}
