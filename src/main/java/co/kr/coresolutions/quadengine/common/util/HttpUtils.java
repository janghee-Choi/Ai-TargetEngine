package co.kr.coresolutions.quadengine.common.util;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class HttpUtils {

	private final RestClient restClient;
	private final RestClient queryPartnerRestClient;

	@Value("${app.running-mode}")
	private String runningMode;

	public <T> T get(String url, Class<T> responseType) {
		return get(restClient, url, null, responseType);
	}

	public <T> T post(String url, @Nullable Object requestBody, Class<T> responseType) {
		return post(restClient, url, null, requestBody, responseType);
	}

	public <T> T getQueryPartner(String url, Class<T> responseType) {
		return get(queryPartnerRestClient, url, null, responseType);
	}

	public <T> T postQueryPartner(String url, @Nullable Object requestBody, Class<T> responseType) {
		return post(queryPartnerRestClient, url, null, requestBody, responseType);
	}

	public <T> T get(RestClient client,
			String url,
			@Nullable Consumer<HttpHeaders> headersCustomizer,
			Class<T> responseType) {

		return client.get()
				.uri(url)
				.headers(h -> {
					if (headersCustomizer != null) headersCustomizer.accept(h);
				})
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					throw new IllegalStateException("HTTP GET failed: " + res.getStatusCode());
				})
				.body(responseType);
	}

	public <T> T post(RestClient client,
			String url,
			@Nullable Consumer<HttpHeaders> headersCustomizer,
			@Nullable Object requestBody,
			Class<T> responseType) {

		return client.post()
				.uri(url)
				.headers(h -> {
					if (headersCustomizer != null) headersCustomizer.accept(h);
				})
				.body(requestBody == null ? "" : requestBody)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					throw new IllegalStateException("HTTP POST failed: " + res.getStatusCode());
				})
				.body(responseType);
	}

	public boolean isRunningModeDuplex() {
		return runningMode.equalsIgnoreCase("duplex");
	}
}
