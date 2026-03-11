package co.kr.coresolutions.quadengine.query.outbound;

public interface PollSink {
	String targetType();

	boolean send(SinkContext context) throws Exception;
}
