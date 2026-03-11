package co.kr.coresolutions.quadengine.scheduler.quartz.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import co.kr.coresolutions.quadengine.scheduler.quartz.listener.JobsListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class QuartzListenerConfig {

    private final Scheduler scheduler;
    private final JobsListener globalJobListener;

    @PostConstruct
    public void registerListener() throws SchedulerException {
        scheduler.getListenerManager()
                .addJobListener(globalJobListener);
    }
}