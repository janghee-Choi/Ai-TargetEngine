package co.kr.coresolutions.quadengine.scheduler.quartz;

import co.kr.coresolutions.quadengine.scheduler.model.ScheduleRequest;
import co.kr.coresolutions.quadengine.scheduler.service.CallApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.text.StringEscapeUtils;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;

@Slf4j
@RequiredArgsConstructor
public class TaskJob extends QuartzJobBean {

	private final ObjectMapper objectMapper;
	private final CallApiService callApiService;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		try {
			JobKey jobKey = context.getJobDetail().getKey();
			String scId = jobKey.getName();
			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

			// JSON 파싱
			ScheduleRequest scheduleRequest = objectMapper.readValue(jobDataMap.get("schedule").toString(),
					ScheduleRequest.class);

			// 이스케이프 문자 처리
			scheduleRequest.setCallString(StringEscapeUtils.unescapeJava(scheduleRequest.getCallString()));

			// 비동기 서비스 호출
			callApiService.asyncCall(scheduleRequest.getCallMode(), scheduleRequest.getCallString(),
					scheduleRequest.getHttpBody());
		} catch (JsonProcessingException e) {
			// Quartz 전용 예외로 래핑하여 던짐
			throw new JobExecutionException("JSON 파싱 에러 발생", e);
		} catch (Exception e) {
			// 기타 발생할 수 있는 런타임 예외 처리
			throw new JobExecutionException("작업 실행 중 예외 발생", e);
		}
	}

}
