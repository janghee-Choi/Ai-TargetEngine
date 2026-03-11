package co.kr.coresolutions.quadengine.scheduler.quartz;

import static org.quartz.JobBuilder.newJob;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.text.StringEscapeUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Component;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.scheduler.model.ScheduleRequest;
import co.kr.coresolutions.quadengine.scheduler.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerManager {

	private final ObjectMapper objectMapper;
	private final Scheduler scheduler;

	private static final String TRIGGER_PREFIX = "TRGR_";
	public static final String JOB_GROUP = "CAMP_JOB";

	public boolean checkExists(String scId) throws SchedulerException {
		JobKey jobKey = new JobKey(scId, JOB_GROUP);
		return scheduler.checkExists(jobKey);
	}

	public void scheduleJob(String scId, ScheduleRequest scheduleRequest) throws SchedulerException, ParseException {
		JobKey jobKey = new JobKey(scId, JOB_GROUP);

		if(!scheduler.checkExists(jobKey)) {
			JobDetail job = null;
			try{
				job = makeJob(jobKey, scheduleRequest);
			}catch(JsonProcessingException e){
				log.error("Failed to create job for scId: {}", scId, e);
				throw new CommonException(ErrorCode.REQUEST_BODY_NOT_VALID, "Failed to create job: " + e.getMessage());
			}
			CronTrigger trigger = makeTrigger(scId, scheduleRequest);
			scheduler.scheduleJob(job, trigger);
		} else {
			throw new CommonException(ErrorCode.SCHEDULE_ALREADY_EXIST);
		}
	}

	public void deleteJob(String scId) throws SchedulerException {
		JobKey jobKey = new JobKey(scId, JOB_GROUP);
		if (scheduler.checkExists(jobKey)) {
			scheduler.deleteJob(jobKey);
		} else {
			throw new CommonException(ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}

	public void rescheduleJob(String scId, ScheduleRequest scheduleRequest) throws SchedulerException, ParseException {
		deleteJob(scId);
		scheduleJob(scId, scheduleRequest);
	}

	private JobDetail makeJob(JobKey jobKey, ScheduleRequest scheduleRequest) throws JsonProcessingException {
		scheduleRequest.setCallString(StringEscapeUtils.escapeJava(scheduleRequest.getCallString()));
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("schedule", objectMapper.writeValueAsString(scheduleRequest));

		return newJob(TaskJob.class)
				.withIdentity(jobKey)
				.usingJobData(jobDataMap)
				.build();
	}

	private CronTrigger makeTrigger(String scId, ScheduleRequest scheduleRequest) throws ParseException {
		Date startDate = null;
		if(!DateUtils.isValidDateTime(scheduleRequest.getStartDttm())) {
			startDate = new SimpleDateFormat(DateUtils.DEF_DATE_FORMAT).parse(DateUtils.getCronExpOrDate(scheduleRequest.getStartDttm(), false));
		} else {
			startDate = new SimpleDateFormat(DateUtils.DEF_DATE_FORMAT).parse(scheduleRequest.getStartDttm());
		}

		Date endDate = null;
		if (!DateUtils.isValidDateTime(scheduleRequest.getEndDttm())) {
			endDate = new SimpleDateFormat(DateUtils.DEF_DATE_FORMAT).parse(DateUtils.getCronExpOrDate(scheduleRequest.getEndDttm(), false));
		} else {
			endDate = new SimpleDateFormat(DateUtils.DEF_DATE_FORMAT).parse(scheduleRequest.getEndDttm());
		}

		return TriggerBuilder.newTrigger()
				.withIdentity(TRIGGER_PREFIX + scId, JOB_GROUP)
				.startAt(startDate)
				.endAt(endDate)
				.withSchedule(CronScheduleBuilder.cronSchedule(scheduleRequest.getSchedule()).withMisfireHandlingInstructionDoNothing())
				.build();
	}

}
