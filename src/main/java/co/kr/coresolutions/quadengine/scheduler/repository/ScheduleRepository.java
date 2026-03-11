package co.kr.coresolutions.quadengine.scheduler.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.exception.ErrorCode;
import co.kr.coresolutions.quadengine.scheduler.domain.Schedule;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleRepository {

	private static final RowMapper<Schedule> TRIGGER_ROW_MAPPER = (rs, rowNum) -> {
		long nextFire = rs.getLong("NEXT_FIRE_TIME");

		LocalDateTime nextFireTime = nextFire == 0 ? null
				: Instant.ofEpochMilli(nextFire)
						.atZone(ZoneId.systemDefault())
						.toLocalDateTime();

		return new Schedule(
				rs.getString("JOB_NAME"),
				rs.getString("JOB_GROUP"),
				rs.getString("JOB_CLASS_NAME"),
				rs.getString("TRIGGER_NAME"),
				rs.getString("TRIGGER_GROUP"),
				rs.getString("TRIGGER_STATE"),
				nextFireTime,
				rs.getString("CRON_EXPRESSION"));
	};

	private final JdbcTemplate jdbcTemplate;

	public List<Schedule> findActiveSchedules() {
		return jdbcTemplate.query(
				"""
						SELECT j.JOB_NAME
						     , j.JOB_GROUP
						     , j.DESCRIPTION
						     , j.JOB_CLASS_NAME
						     , t.TRIGGER_NAME
						     , t.TRIGGER_GROUP
						     , t.TRIGGER_STATE
							 , t.NEXT_FIRE_TIME
						     , c.CRON_EXPRESSION
						  FROM T_SCHEDULE_JOB_DETAILS j
						  JOIN T_SCHEDULE_TRIGGERS t
						    ON j.SCHED_NAME = t.SCHED_NAME
						   AND j.JOB_NAME = t.JOB_NAME
						   AND j.JOB_GROUP = t.JOB_GROUP
						  LEFT JOIN T_SCHEDULE_CRON_TRIGGERS c
						    ON t.SCHED_NAME = c.SCHED_NAME
						   AND t.TRIGGER_NAME = c.TRIGGER_NAME
						   AND t.TRIGGER_GROUP = c.TRIGGER_GROUP
						 WHERE t.TRIGGER_STATE IN ('WAITING', 'ACQUIRED')
						""",
				TRIGGER_ROW_MAPPER);
	}

	public Schedule findSchedule(String jobName, String jobGroup) {
		try {
			return jdbcTemplate.queryForObject(
					"""
							SELECT j.JOB_NAME
								, j.JOB_GROUP
								, j.JOB_CLASS_NAME
								, t.TRIGGER_NAME
								, t.TRIGGER_GROUP
								, t.TRIGGER_STATE
								, t.NEXT_FIRE_TIME
								, c.CRON_EXPRESSION
							FROM T_SCHEDULE_JOB_DETAILS j
							JOIN T_SCHEDULE_TRIGGERS t
								ON j.SCHED_NAME = t.SCHED_NAME
							AND j.JOB_NAME = t.JOB_NAME
							AND j.JOB_GROUP = t.JOB_GROUP
							LEFT JOIN T_SCHEDULE_CRON_TRIGGERS c
								ON t.SCHED_NAME = c.SCHED_NAME
							AND t.TRIGGER_NAME = c.TRIGGER_NAME
							AND t.TRIGGER_GROUP = c.TRIGGER_GROUP
							WHERE j.JOB_NAME = ?
							AND j.JOB_GROUP = ?
							""",
					TRIGGER_ROW_MAPPER,
					jobName,
					jobGroup
			);
		} catch (EmptyResultDataAccessException e) {
			throw new CommonException(ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}
}
