package co.kr.coresolutions.quadengine.query.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PollingTarget {

	@JsonProperty("DP_ID")
	private String dpId;
	
	@JsonProperty("DP_OTPT_SEQ")
	private Integer dpOutSeq;
	
	@JsonProperty("DP_OTPT_TYPE")
	private String dpOutType;
	
	@JsonProperty("DP_TRGT_TYPE")
	private String dpTarget;
	
	@JsonProperty("DP_TBL_NM")
	private String dpTableName;
	
	@JsonProperty("DP_SAVE_MTHD")
	private String dpOutMode;

	@JsonProperty("DP_TBL_KEY")
	private String dpTableKey;

	@JsonProperty("DP_FILE_DLM")
	private String dpFileSEP;

	@JsonProperty("DP_FILE_CRT_CYCLE")
	private String dpFileRepeat;

	@JsonProperty("DP_TRGT_CONN_INFO")
	private String outConnId;

	@JsonProperty("DP_DML_SQL")
	private String dmlSql;

	@JsonProperty("DP_MID_CONN_INFO")
	private String midConnId;

}
