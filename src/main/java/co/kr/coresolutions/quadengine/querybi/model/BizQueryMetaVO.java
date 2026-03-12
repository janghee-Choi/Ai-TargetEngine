package co.kr.coresolutions.quadengine.querybi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY) // 데이터가 없는 리스트는 JSON 응답에서 제외
public class BizQueryMetaVO {

	private String queryId;

	private int seq;

	private int joinId;

	private String queryMeta;

	private String fieldName;

	private String dbmsId;

	private String outTableId;

	@Builder.Default
	private List<BizQueryPromptVO> bizQueryPromptList = new ArrayList<>();

	@Builder.Default
	private List<BizTargetingVO> bizTargetingList = new ArrayList<>();

	public void addBizQueryPrompt(BizQueryPromptVO vo) {
		if (this.bizQueryPromptList == null) {
			this.bizQueryPromptList = new ArrayList<>();
		}
		this.bizQueryPromptList.add(vo);
	}

	public void addBizTargeting(BizTargetingVO vo) {
		if (this.bizTargetingList == null) {
			this.bizTargetingList = new ArrayList<>();
		}
		this.bizTargetingList.add(vo);
	}
}
