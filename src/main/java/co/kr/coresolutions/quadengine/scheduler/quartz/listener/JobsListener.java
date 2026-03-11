package co.kr.coresolutions.quadengine.scheduler.quartz.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobsListener implements JobListener {

    private final QueryService queryService;

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /*
     * Job이 실행되기 전 호출되는 메서드
     * Job 실행 시점
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        try {
            String contextJson = context.getJobDetail().getJobDataMap().get("schedule").toString();
            String insertSql = """
                    INSERT INTO
                        T_SCHEDULE_HIST ( SCHEDULER_NAME,
                                          INSTANCE_NAME,
                                          JOB_GROUP,
                                          JOB_NAME,
                                          TRIGGER_NAME,
                                          TRIGGER_GROUP,
                                          FIRE_INSTANCE_ID,
                                          SCHEDULED_FIRE_TIME,
                                          START_TIME,
                                          STATUS,
                                          CONTEXT_JSON)
                                 VALUES (:schedulerName,
                                         :instanceName,
                                         :jobGroup,
                                         :jobName,
                                         :triggerName,
                                         :triggerGroup,
                                         :fireInstanceId,
                                         :scheduledFireTime,
                                         CURRENT_TIMESTAMP,
                                         :status,
                                         :contextJson)
                    """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("schedulerName", context.getScheduler().getSchedulerName())
                    .addValue("instanceName", context.getScheduler().getSchedulerInstanceId())
                    .addValue("jobGroup", context.getJobDetail().getKey().getGroup())
                    .addValue("jobName", context.getJobDetail().getKey().getName())
                    .addValue("triggerName", context.getTrigger().getKey().getName())
                    .addValue("triggerGroup", context.getTrigger().getKey().getGroup())
                    .addValue("fireInstanceId", context.getFireInstanceId())
                    .addValue("scheduledFireTime", context.getScheduledFireTime())
                    .addValue("status", "START")
                    .addValue("contextJson", contextJson);

            if(queryService.update(insertSql, params) < 1) {
                throw new CommonException(ErrorCode.SCHEDULE_HIST_INSERT_FAIL);
            }
        } catch(SchedulerException e) {
            throw new CommonException(ErrorCode.SCHEDULE_HIST_ERROR);
        }
    }

    /*
     * 다른 JobListener가 Job의 실행을 방해했을때 호출되는 메서드
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        log.info("jobExecutionVetoed"); // 트리거 veto 실행시
    }

    /*
     * Job의 실행이 완료된 후에 호출되는 메서드
     * Job 종료 시점
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
        String successYn = (e == null) ? "SUCCESS" : "FAIL";
        String insertSql = """
                    UPDATE
                        T_SCHEDULE_HIST
                    SET
                        END_TIME = CURRENT_TIMESTAMP,
                        STATUS = :status,
                        SUCCESS_YN = :successYn,
                        ERROR_CODE = :errorCode,
                        ERROR_MESSAGE = :errorMessage,
                        SPEND_TIME = :spendTime
                    WHERE
                        FIRE_INSTANCE_ID = :fireInstanceId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", "END")
                .addValue("successYn", successYn)
                .addValue("errorCode", (e != null) ? "SCHEDULE_JOB_ERROR" : null)
                .addValue("errorMessage", (e != null) ? e.getMessage() : null)
                .addValue("spendTime", context.getJobRunTime())
                .addValue("fireInstanceId", context.getFireInstanceId());

        if (queryService.update(insertSql, params) < 1) {
            throw new CommonException(ErrorCode.SCHEDULE_HIST_INSERT_FAIL);
        }
    }

}