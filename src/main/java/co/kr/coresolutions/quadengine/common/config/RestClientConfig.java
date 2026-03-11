package co.kr.coresolutions.quadengine.common.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Configuration
public class RestClientConfig {

	@Value("${rest-client.config.max-conn-total}")
	private int maxConnTotal;

	@Value("${rest-client.config.max-conn-per-route}")
	private int maxConnPerRoute;

	@Value("${rest-client.config.connect-timeout}")
	private long connectTimeout;

	@Value("${rest-client.config.socket-timeout}")
	private long socketTimeout;

	@Value("${rest-client.config.connection-request-timeout}")
	private long connectionRequestTimeout;

	@Value("${rest-client.config.evict-idle-connections}")
	private long evictIdleConnections;

	@Value("${app.partner.url}")
	private String partnerUrl;

	@Bean
	public CloseableHttpClient httpClient() {
		// TCP 연결
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setConnectTimeout(Timeout.ofSeconds(connectTimeout))
				.build();

		// 응답 대기
		SocketConfig socketConfig = SocketConfig.custom()
				.setSoTimeout(Timeout.ofSeconds(socketTimeout))
				.build();

		// Pool 설정
		PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
				.setMaxConnTotal(maxConnTotal)
				.setMaxConnPerRoute(maxConnPerRoute)
				.setDefaultConnectionConfig(connectionConfig)
				.setDefaultSocketConfig(socketConfig)
				.build();

		// Pool 대기
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofSeconds(connectionRequestTimeout))
				.build();

		return HttpClients.custom()
				.setConnectionManager(cm)
				.setDefaultRequestConfig(requestConfig)
				.evictExpiredConnections()
				.evictIdleConnections(TimeValue.ofSeconds(evictIdleConnections))
				.build();
	}

	@Bean
	public HttpComponentsClientHttpRequestFactory httpRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory(httpClient());
	}

	@Bean
	public ClientHttpRequestInterceptor loggingRequestInterceptor() {
		return (request, body, execution) -> {
			long startNs = System.nanoTime();

			// 로그 설정
			log.info(
					"""
							
							[ Request ]
							{} {}
							Attributes : {}
							""",
					request.getMethod(),
					request.getURI(),
					request.getAttributes()
			);

			ClientHttpResponse response;
			try {
				response = execution.execute(request, body);
				long tookMs = Duration.ofNanos(System.nanoTime() - startNs).toMillis();

				log.info(
						"""
								
								[ Response ]
								{} {} ({}ms)
								Status code : {}
								Body : {}
								""",
						request.getMethod(),
						request.getURI(),
						tookMs,
						response.getStatusCode(),
						response.getBody()
				);

				return response;
			} catch (IOException e) {
				long tookMs = Duration.ofNanos(System.nanoTime() - startNs).toMillis();
				log.warn(
						"""
								
								[ Error ]
								{} {} ({}ms)
								Error Message :{}
								""",
						request.getMethod(),
						request.getURI(),
						tookMs,
						e.toString()
				);
				throw e;
			}
		};
	}

	@Bean
	public RestClient restClient() {
		return RestClient.builder()
				.requestFactory(httpRequestFactory())
				.requestInterceptor(loggingRequestInterceptor())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	@Bean
	public RestClient queryPartnerRestClient() {
		return RestClient.builder()
				.baseUrl(partnerUrl)
				.requestFactory(httpRequestFactory())
				.requestInterceptor(loggingRequestInterceptor())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
