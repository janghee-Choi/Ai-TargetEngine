package co.kr.coresolutions.quadengine.query.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class PollingAsyncConfig {

	@Bean(name = "pollingTaskExecutor")
	public Executor pollingTaskExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

}
