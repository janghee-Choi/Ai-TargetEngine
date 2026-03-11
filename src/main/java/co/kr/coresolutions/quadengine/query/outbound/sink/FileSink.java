package co.kr.coresolutions.quadengine.query.outbound.sink;

import co.kr.coresolutions.quadengine.query.domain.PollingTarget;
import co.kr.coresolutions.quadengine.query.outbound.PollSink;
import co.kr.coresolutions.quadengine.query.outbound.SinkContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class FileSink implements PollSink {

	@Override
	public String targetType() {
		return "FILE";
	}

	@Override
	public boolean send(SinkContext context) throws Exception {
		PollingTarget target = context.target();
		List<Map<String, Object>> originList = context.originList();

		return true;
	}
}
