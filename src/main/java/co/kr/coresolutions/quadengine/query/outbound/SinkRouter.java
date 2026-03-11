package co.kr.coresolutions.quadengine.query.outbound;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SinkRouter {

	private final Map<String, PollSink> sinkMap;

	public SinkRouter(List<PollSink> sinks) {
		this.sinkMap = sinks.stream().collect(Collectors.toUnmodifiableMap(
				sink -> sink.targetType().toUpperCase(Locale.ROOT),
				Function.identity()
		));
	}

	public PollSink route(String dpTarget) {
		if (dpTarget == null) {
			throw new IllegalArgumentException("dpTarget is null");
		}
		PollSink sink = sinkMap.get(dpTarget.toUpperCase(Locale.ROOT));
		if (sink == null) {
			throw new IllegalArgumentException("Unsupported dpTarget: " + dpTarget);
		}
		return sink;
	}
}
