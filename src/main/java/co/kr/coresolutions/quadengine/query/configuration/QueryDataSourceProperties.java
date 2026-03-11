package co.kr.coresolutions.quadengine.query.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "query.data-source.config")
public class QueryDataSourceProperties {
	private int maximumPoolSize;
	private int minimumIdle;
	private int connectionTimeout;
	private int idleTimeout;
	private int maxLifetime;
	private int leakDetectionThreshold;
}
