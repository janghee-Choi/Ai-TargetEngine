package co.kr.coresolutions.quadengine.query.outbound;

import co.kr.coresolutions.quadengine.query.domain.PollingTarget;
import co.kr.coresolutions.quadengine.query.executor.PollingExecutor;

import java.util.List;
import java.util.Map;

public record SinkContext(
		PollingExecutor executor,
		PollingTarget target,
		List<Map<String, Object>> originList,
		String dpSqlField,
		String messageId
) {
}
