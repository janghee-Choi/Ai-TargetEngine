package co.kr.coresolutions.quadengine.scheduler.controller;

import co.kr.coresolutions.quadengine.common.event.ScheduleEvent;
import co.kr.coresolutions.quadengine.scheduler.model.ScheduleRequest;
import co.kr.coresolutions.quadengine.scheduler.service.SchedulerService;
import co.kr.coresolutions.quadengine.scheduler.validation.ScheduleRequestValidator;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
@RequiredArgsConstructor
public class ScheduleEventHandler {

	private final SchedulerService schedulerService;
	private final ScheduleRequestValidator scheduleRequestValidator;

	@EventListener
	public void handleScheduleEvent(ScheduleEvent event) throws SchedulerException, ParseException {
		ScheduleRequest scheduleRequest = ScheduleRequest.builder()
				.scId(event.scId())
				.startDttm(event.startDttm())
				.endDttm(event.endDttm())
				.name(event.name())
				.schedule(event.schedule())
				.callMode(event.callMode())
				.callString(event.callString())
				.created(event.created())
				.updated(event.updated())
				.owner(event.owner())
				.httpBody(event.httpBody())
				.next(event.next())
				.build();

		scheduleRequestValidator.validate(scheduleRequest);
		schedulerService.addSchedule(event.scId(), scheduleRequest);
	}

}
