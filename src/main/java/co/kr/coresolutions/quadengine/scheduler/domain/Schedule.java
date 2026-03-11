package co.kr.coresolutions.quadengine.scheduler.domain;

import java.time.LocalDateTime;

public record Schedule(
		String jobName,
		String jobGroup,
		String jobClassName,
		String triggerName,
		String triggerGroup,
		String triggerState,
		LocalDateTime nextFireTime,
		String cronExpression
) {
}