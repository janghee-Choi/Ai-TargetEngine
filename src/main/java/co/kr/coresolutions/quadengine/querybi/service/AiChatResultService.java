package co.kr.coresolutions.quadengine.querybi.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import co.kr.coresolutions.quadengine.common.exception.CommonException;
import co.kr.coresolutions.quadengine.common.util.SqlUtils;
import co.kr.coresolutions.quadengine.query.service.QueryService;
import co.kr.coresolutions.quadengine.querybi.dto.AiChatResultDto;
import co.kr.coresolutions.quadengine.querybi.enums.AiChatErrorCode;


@Service
@RequiredArgsConstructor
public class AiChatResultService {

    private final QueryService queryService;

    public List<AiChatResultDto> getResultsBySessionId(String sessionId) {
        String query = """
				select
                    sessionid,
                    keyid,
                    audience_id,
                    userid,
                    result,
                    version
                from
                    quadmax.t_ssbi_aichat_tresult
                where
                    sessionid = :sessionId
                order by
                    keyid,seq
				""";
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("sessionId", sessionId);

		List<Map<String,Object>> ResultMapList = queryService.selectList(query, params);

        List<AiChatResultDto> AiChatResultList = SqlUtils.mapToList(ResultMapList, AiChatResultDto.class);

		if (AiChatResultList.isEmpty()) {
			throw new CommonException(AiChatErrorCode.AI_CHAT_INVALID_REQUEST, "Session not found : " + sessionId);
		}
        return AiChatResultList;
    }
}