package co.kr.coresolutions.quadengine.querybi.service; // 적절한 패키지로 변경

import co.kr.coresolutions.quadengine.querybi.model.BizQueryMetaVO;
import co.kr.coresolutions.quadengine.querybi.model.BizQueryPromptVO;
import co.kr.coresolutions.quadengine.querybi.model.BizTargetingVO;
import co.kr.coresolutions.quadengine.querybi.sqlMaker.SqlMakerFactory;
import co.kr.coresolutions.quadengine.querybi.sqlMaker.SqlMaker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class BizQueryBuliderService {

    // 정적 필드 대신 컨텍스트 객체나 파라미터 사용 권장 (예시용 유지)
    private final List<BizQueryMetaVO> bizQueryMetaList = new ArrayList<>();

    public void addBizQueryMeta(BizQueryMetaVO vo) {
        this.bizQueryMetaList.add(vo);
    }

    BizQueryBuliderService(List<BizQueryMetaVO> bizQueryMetaList) {
        this.bizQueryMetaList.addAll(bizQueryMetaList);
    }

    /**
     * 전체 쿼리 치환 프로세스 실행
     */
    public void replaceQuery() {
        bizQueryMetaList.forEach(meta -> {
            setReplaceInfo(meta);
            replaceWhereString(meta);
            replaceSelectString(meta);
        });
    }

    /**
     * 최종 SQL 생성
     */
    public String makeSql(boolean dbmsEqualsF) {
        StringBuilder sb = new StringBuilder();
        int totalSize = bizQueryMetaList.size();

        for (int i = 0; i < totalSize; i++) {
            BizQueryMetaVO meta = bizQueryMetaList.get(i);
            int currentIdx = i + 1;
            boolean isStart = (i == 0);
            boolean isLast = (currentIdx == totalSize);

            // 1. 쿼리 원문 또는 아웃테이블 쿼리 결정
            String query = "";
            if (dbmsEqualsF) {
                query = meta.getQueryMeta();
            } else {
                Pattern pattern = Pattern.compile("(.+)_(.+)_(//d+)");
                Matcher matcher = pattern.matcher(meta.getOutTableId());

                if (matcher.matches()) {
                    String sessionId = matcher.group(1);
                    String audienceId = matcher.group(2);
                    int seq = Integer.parseInt(matcher.group(3));

                    log.info("Parsed -> SessionId: {}, AudienceId: {}, Seq: {}", sessionId, audienceId, seq);
                    query = "SELECT %s FROM T_AI_TARGET_RESULT WHERE SESSION_ID = '%s' AND AUDIENCE_ID = '%s'".formatted(meta.getFieldName(), sessionId, audienceId);
                }

            }
            // 2. Factory를 통한 SqlMaker 생성 및 SQL 조립
            SqlMaker sqlMaker = SqlMakerFactory.getInstance().create(meta.getJoinId(), meta.getFieldName(), currentIdx,query, isStart, isLast);

            sb.append(sqlMaker.makeSql());

            // 3. 가독성을 위한 줄바꿈 (선택)
            if (!isLast)
                sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * WHERE 조건절 치환
     */
    public void replaceWhereString(BizQueryMetaVO meta) {
        // 1차 치환: 기본 프롬프트 형태 치환
        for (BizQueryPromptVO prompt : meta.getBizQueryPromptList()) {
            String query = meta.getQueryMeta();
            if (prompt.isRemoveF()) {
                query = query.replaceFirst(Pattern.quote(prompt.getPromptString()), "");
            } else if (!prompt.isSkip()) {
                String replaceStr = prompt.getPromptString().replace("[", "").replace("]", "").replaceAll("(?i)::op::",
                        prompt.getReplacePromptOp(true));

                replaceStr = replaceStr.replace(prompt.getPromptKeyword(), prompt.getPromptOpValue());
                query = query.replace(prompt.getPromptString(), replaceStr);
            }
            meta.setQueryMeta(query);
        }

        // 2차 치환: 특수 키워드(@@, ##, $$) 치환
        for (BizQueryPromptVO prompt : meta.getBizQueryPromptList()) {
            if (!prompt.isRemoveF() && !prompt.isSkip()) {
                String query = meta.getQueryMeta();
                String rawKeyword = prompt.getPromptKeyword().replaceAll("[#@$]", "");

                query = query.replace("@@" + rawKeyword + "@@", prompt.getPromptOpValue(1))
                        .replace("##" + rawKeyword + "##", prompt.getPromptOpValue(2))
                        .replace("$$" + rawKeyword + "$$", prompt.getPromptOpValue(3));

                meta.setQueryMeta(query);
            }
        }
    }

    /**
     * SELECT 대상 컬럼 치환
     */
    public void replaceSelectString(BizQueryMetaVO meta) {
        String query = meta.getQueryMeta().replace("@@SELECT_STRING@@", meta.getFieldName());
        meta.setQueryMeta(query);
    }

    /**
     * 타겟팅 정보 매칭 및 프롬프트 설정
     */
    public void setReplaceInfo(BizQueryMetaVO meta) {
        for (BizQueryPromptVO prompt : meta.getBizQueryPromptList()) {
            meta.getBizTargetingList().stream()
                    .filter(target -> prompt.getPromptKeyword().equals(target.getPromptKeyword())).findFirst()
                    .ifPresentOrElse(target -> {
                        prompt.setRemoveF(false);
                        prompt.setSkip(false);
                        prompt.setReplacePromptOp(target.getPromptOp());
                        prompt.setReplacePromptType(target.getPromptType());
                        prompt.setReplacePromptValue(target.getPromptValue());
                    }, () -> {
                        if (prompt.isRemoveF() && prompt.isRequired()) {
                            prompt.setRemoveF(false);
                            prompt.setSkip(true);
                        }
                    });
        }
    }

    /**
     * 모든 DBMS ID가 동일한지 확인
     */
    public boolean equalsDbms() {
        if (bizQueryMetaList.isEmpty())
            return true;
        String firstId = bizQueryMetaList.get(0).getDbmsId();
        return bizQueryMetaList.stream().allMatch(meta -> meta.getDbmsId().equals(firstId));
    }
}
