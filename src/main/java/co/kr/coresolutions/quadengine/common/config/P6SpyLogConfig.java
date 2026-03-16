package co.kr.coresolutions.quadengine.common.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import com.p6spy.engine.spy.P6SpyOptions;
import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "decorator.datasource.p6spy.enable-logging", havingValue = "true")
public class P6SpyLogConfig {

    @PostConstruct
    public void setLogMessageFormat() {
        // P6Spy 포맷터를 커스텀 클래스로 교체
        P6SpyOptions.getActiveInstance().setLogMessageFormat(P6spyPrettySqlFormat.class.getName());
    }

    public static class P6spyPrettySqlFormat implements MessageFormattingStrategy {
        @Override
        public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared,
                String sql, String url) {
            if (sql == null || sql.trim().isEmpty())
                return "";

            // 모든 줄바꿈과 연속된 공백을 단일 공백으로 치환 (한 줄 만들기)
            String cleanSql = sql.replaceAll("\\s+", " ").trim();

            // 출력 형식: [시간]ms | SQL
            return elapsed + "ms | " + cleanSql;
        }
    }
}