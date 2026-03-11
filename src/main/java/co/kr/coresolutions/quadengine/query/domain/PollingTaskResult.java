package co.kr.coresolutions.quadengine.query.domain;

public record PollingTaskResult(
		Integer dpOutSeq,
		boolean success,
		int records,
		String message
) {}
