package co.kr.coresolutions.quadengine.querybi.model; // 프로젝트 패키지 구조에 맞게 수정

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BizTargetingVO {

	private String queryId;

	private int seq;

	private int joinId;

	private String promptString;

	private String promptKeyword;

	private String promptValue;

	private String promptType;

	private String promptOp;

}