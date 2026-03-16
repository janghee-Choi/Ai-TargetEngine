package co.kr.coresolutions.quadengine.querybi.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import co.kr.coresolutions.quadengine.common.util.SqlUtils;
import co.kr.coresolutions.quadengine.query.service.QueryService;
import co.kr.coresolutions.quadengine.querybi.dto.AiChatResultDto;
import co.kr.coresolutions.quadengine.querybi.interfaces.ParseResult;

@Service
@RequiredArgsConstructor
public class AiChatResultService {

    private final QueryService queryService;

    public ParseResult<List<AiChatResultDto>> getResultsBySessionId(String sessionId) {
        String query = """
                SELECT
                    sessionid,
                    keyid,
                    audience_id,
                    userid,
                    result,
                    version
                FROM
                    quadmax.t_ssbi_aichat_tresult
                WHERE
                    sessionid = :sessionId
                ORDER BY
                    keyid, seq
                """;

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("sessionId", sessionId);

        List<Map<String, Object>> resultMapList = queryService.selectList(query, params);

        if (resultMapList.isEmpty()) {
            return ParseResult.empty();
        }

        List<AiChatResultDto> aiChatResultList = SqlUtils.mapToList(resultMapList, AiChatResultDto.class);
        return ParseResult.success(aiChatResultList, null);
    }
}