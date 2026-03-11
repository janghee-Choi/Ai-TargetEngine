package co.kr.coresolutions.quadengine.scheduler.service;

import co.kr.coresolutions.quadengine.scheduler.domain.Schedule;
import co.kr.coresolutions.quadengine.scheduler.model.ScheduleRequest;
import co.kr.coresolutions.quadengine.scheduler.quartz.SchedulerManager;
import co.kr.coresolutions.quadengine.scheduler.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerService {

	private final SchedulerManager schedulerManager;
	private final ScheduleRepository scheduleRepository;

	public List<Schedule> activeSchedules() {
		return scheduleRepository.findActiveSchedules();
	}

	public Schedule getSchedule(String scId) {
		return scheduleRepository.findSchedule(scId, SchedulerManager.JOB_GROUP);
	}

	public void addSchedule(String scId, ScheduleRequest scheduleRequest) throws SchedulerException, ParseException {
		if (!schedulerManager.checkExists(scId)) {
			schedulerManager.scheduleJob(scId, scheduleRequest);
		}
	}

	public void updateSchedule(String scId, ScheduleRequest scheduleRequest) throws SchedulerException, ParseException {
		schedulerManager.rescheduleJob(scId, scheduleRequest);
	}

	public void deleteSchedule(String scId) throws SchedulerException {
		schedulerManager.deleteJob(scId);
	}

}
