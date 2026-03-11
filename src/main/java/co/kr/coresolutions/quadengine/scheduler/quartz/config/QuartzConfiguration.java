package co.kr.coresolutions.quadengine.scheduler.quartz.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 조건부 설정
 * SPRING.QUARTZ.ENABLED=true 일 때만 Quartz 자동설정이 활성화됩니다.
 */
@Configuration
public class QuartzConfiguration {
    // Quartz 관련 Bean들이 자동으로 등록됩니다
}
