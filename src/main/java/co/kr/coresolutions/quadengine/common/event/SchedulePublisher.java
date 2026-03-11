package co.kr.coresolutions.quadengine.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class SchedulePublisher {

	private final ApplicationEventPublisher publisher;

	public void sendScheduleEvent(ScheduleEvent scheduleEvent) {
		publisher.publishEvent(scheduleEvent);
	}

	public void sendScheduleEvent(
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
	) {
		publisher.publishEvent(new ScheduleEvent(
				scId, startDttm, endDttm, name, schedule, callMode, callString,
				created, updated, owner, httpBody, next
		));
	}

}
