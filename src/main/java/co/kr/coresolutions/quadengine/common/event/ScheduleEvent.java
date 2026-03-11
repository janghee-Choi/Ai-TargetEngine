package co.kr.coresolutions.quadengine.common.event;

import com.fasterxml.jackson.databind.JsonNode;

public record ScheduleEvent(
		String scId,
		String startDttm,
		String endDttm,
		String name,
		String schedule,
		String callMode,
		String callString,
		String created,
		String updated,
		String owner,
		JsonNode httpBody,
		String next
) {}
