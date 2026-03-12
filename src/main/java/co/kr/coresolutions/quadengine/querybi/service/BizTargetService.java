package co.kr.coresolutions.quadengine.querybi.service;

import co.kr.coresolutions.quadengine.common.util.SqlUtils;
import co.kr.coresolutions.quadengine.query.configuration.ConnectionInfo;
import co.kr.coresolutions.quadengine.query.configuration.DataSourceConfig;
import co.kr.coresolutions.quadengine.query.service.QueryService;
import co.kr.coresolutions.quadengine.querybi.dto.AudienceDto;
import co.kr.coresolutions.quadengine.querybi.dto.BizTargetResultDto;
import co.kr.coresolutions.quadengine.querybi.dto.SetOperationDto;
import co.kr.coresolutions.quadengine.querybi.dto.TMetaResultDto;
import co.kr.coresolutions.quadengine.querybi.model.BizQueryMetaVO;
import co.kr.coresolutions.quadengine.querybi.model.BizQueryPromptVO;
import co.kr.coresolutions.quadengine.querybi.model.BizTargetingVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; // 추가됨
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import org.springframework.context.ApplicationContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class BizTargetService {

    private final QueryService queryService;
    private final ObjectMapper objectMapper;
    private final DataSourceConfig dataSourceConfig;

    private final ApplicationContext applicationContext;

    // 성능을 위해 정규식 패턴을 상수로 선언
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\r\\n|\\n");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s]+");
    private static final Pattern OP_PATTERN = Pattern.compile("(?i)::op::"); // 대소문자 무시 (::op:: | ::OP::)

    public String buildSetOpResultSql(String sessionId, AudienceDto audienceDto) {
        if (audienceDto == null || audienceDto.getSetOperation() == null) {
            return "";
        }

        SetOperationDto setOperationDto = audienceDto.getSetOperation();

        // 재귀 호출 시작 (최상위 setOperation 노드 전달)
        return buildSetOpResultSql(sessionId, setOperationDto);
    }

    public String buildSetOpResultSql(String sessionId, Object node) {
        // 1. 노드가 문자열(Audience ID)인 경우: 최하위 노드 도달 ("A", "B" 등)
        if (node instanceof String aid) {
            return """
                    SELECT CUST_ID FROM T_AI_TARGET_RESULT
                    WHERE SESSION_ID = '%s' AND AUDIENCE_ID = '%s'
                    """.formatted(sessionId, aid).trim();
        }

        // 2. 노드가 SetOperationDto 객체이거나 Map인 경우 데이터 추출
        String type = "";
        List<?> operands = null;
        Object left = null;
        Object right = null;

        if (node instanceof SetOperationDto op) {
            type = Optional.ofNullable(op.getType()).orElse("").toUpperCase();
            operands = op.getOperands();
            left = op.getLeft();
            right = op.getRight();
        } else if (node instanceof Map<?, ?> opMap) {
            type = Optional.ofNullable(opMap.get("type")).map(Object::toString).orElse("").toUpperCase();
            operands = (List<?>) opMap.get("operands");
            left = opMap.get("left");
            right = opMap.get("right");
        }

        // 3. 연산 처리 (Java 21 Switch Expression)
        return switch (type) {
        case "OR", "UNION" -> {
            if (operands == null || operands.isEmpty())
                yield "";
            String unionBody = operands.stream().map(o -> buildSetOpResultSql(sessionId, o)).filter(s -> !s.isBlank())
                    .collect(Collectors.joining(") UNION (", "(", ")"));
            yield "SELECT CUST_ID FROM (" + unionBody + ") AS T_UNION";
        }
        case "AND", "INTERSECTION", "INTERSECT" -> {
            if (left == null && operands != null && operands.size() >= 2) {
                left = operands.get(0);
                right = operands.get(1);
            }
            String leftSql = buildSetOpResultSql(sessionId, left);
            String rightSql = buildSetOpResultSql(sessionId, right);
            yield (leftSql.isBlank() || rightSql.isBlank()) ? ""
                    : "SELECT CUST_ID FROM (%s) AS T_L WHERE CUST_ID IN (SELECT CUST_ID FROM (%s) AS T_R)"
                            .formatted(leftSql, rightSql);
        }
        case "DIFFERENCE", "EXCEPT", "MINUS" -> {
            if (left == null && operands != null && operands.size() >= 2) {
                left = operands.get(0);
                right = operands.get(1);
            }
            String leftSql = buildSetOpResultSql(sessionId, left);
            String rightSql = buildSetOpResultSql(sessionId, right);
            yield (leftSql.isBlank() || rightSql.isBlank()) ? ""
                    : "SELECT CUST_ID FROM (%s) AS T_L WHERE CUST_ID NOT IN (SELECT CUST_ID FROM (%s) AS T_R)"
                            .formatted(leftSql, rightSql);
        }
        case "SINGLE" -> (operands != null && !operands.isEmpty()) ? buildSetOpResultSql(sessionId, operands.getFirst())
                : "";
        default -> "";
        };
    }

    public BizTargetResultDto getBizTargetService(String sessionId, String audienceId,
            Map<String, List<TMetaResultDto>> tMetaQueryGroupedDtoList) {

        BizTargetResultDto resultDto = BizTargetResultDto.builder().sessionId(sessionId).audienceId(audienceId)
                .tMetaResultList(tMetaQueryGroupedDtoList.values().stream().flatMap(List::stream).toList())
                .targetSuccess(false).build();

        // 1. 임시 테이블명 생성 (예: TGT_타겟ID_세션ID)
        // 세션ID나 타겟ID에 특수문자가 있을 수 있으므로 정제하여 사용하거나 audienceId를 직접 사용
        String safeSessionId = Optional.ofNullable(sessionId).orElse("NONE").replaceAll("[^a-zA-Z0-9]", "_") // 영문/숫자 아닌
                                                                                                             // 문자를 _로
                                                                                                             // 변경
                .replaceAll("_{2,}", "_"); // 연속된 언더바를 하나로 축소

        String outTableName = "TGT_" + audienceId + "_" + safeSessionId;
        resultDto.setOutTableName(outTableName);

        if (tMetaQueryGroupedDtoList == null || tMetaQueryGroupedDtoList.isEmpty()) {
            return resultDto;
        }

        try {
            List<String> queryIdList = tMetaQueryGroupedDtoList.keySet().stream().toList();
            MapSqlParameterSource params = new MapSqlParameterSource().addValue("queryIdList", queryIdList);

            // 1. BizQueryMeta 조회
            String bizQueryMetaSql = """
                    SELECT XQL.Qry_ID AS queryId, ROW_NUMBER() OVER (ORDER BY Qry_SEQ ASC) as seq,
                           1 as joinId, 'CUST_ID' as fieldName, XQL.Qry_DBMS_ID AS dbmsId, XQL.QRY_META as queryMeta
                    FROM T_XLIG_QUERY_LIST XQL
                    WHERE XQL.DEL_F = 'N' AND QRY_TYPE = 'T' AND XQL.Qry_ID IN (:queryIdList) AND XQL.LANG = 'ko_KR'
                    ORDER BY XQL.Qry_SEQ ASC
                    """;

            List<BizQueryMetaVO> bizQueryMetaList = SqlUtils.mapToList(queryService.selectList(bizQueryMetaSql, params),
                    BizQueryMetaVO.class);

            // 2. BizQueryPrompt 조회 및 그룹화
            String bizQueryPromptSql = """
                    SELECT XQP.QRY_ID AS queryId, XQP.SEQ AS seq, XQP.PRMP_STRING AS promptString, XQP.PRMP_KWD AS promptKeyword, XQP.PRMP_OP AS promptOp
                    FROM T_XLIG_QUERY_PROMPT XQP
                    WHERE XQP.QRY_ID IN (:queryIdList)
                    """;

            List<BizQueryPromptVO> bizQueryPromptList = SqlUtils
                    .mapToList(queryService.selectList(bizQueryPromptSql, params), BizQueryPromptVO.class);
            var promptGroupMap = bizQueryPromptList.stream()
                    .collect(Collectors.groupingBy(BizQueryPromptVO::getQueryId));

            // 3. BizTargeting 리스트 생성 및 그룹화
            AtomicInteger seqCounter = new AtomicInteger(1);
            var targetingGroupMap = tMetaQueryGroupedDtoList.values().stream().flatMap(List::stream).map(dto -> {
                BizTargetingVO vo = new BizTargetingVO();
                vo.setQueryId(dto.getQueryId());
                vo.setSeq(seqCounter.getAndIncrement());
                vo.setJoinId(1);
                vo.setPromptKeyword(dto.getFilterId());
                vo.setPromptOp(dto.getOperator());
                vo.setPromptType("STRING");
                vo.setPromptValue(dto.getValue());
                return vo;
            }).collect(Collectors.groupingBy(BizTargetingVO::getQueryId));

            // 4. 최종 조립
            bizQueryMetaList.forEach(meta -> {
                String qid = meta.getQueryId();
                Optional.ofNullable(promptGroupMap.get(qid)).ifPresent(list -> list.forEach(meta::addBizQueryPrompt));
                Optional.ofNullable(targetingGroupMap.get(qid)).ifPresent(list -> list.forEach(meta::addBizTargeting));
            });

            // 5. 빌더 호출 및 최종 SQL 생성
            BizQueryBuliderService builderService = new BizQueryBuliderService(bizQueryMetaList);
            builderService.replaceQuery();

            javax.sql.DataSource outDs = applicationContext.getBean(javax.sql.DataSource.class);

            String rawSql = "";
            if (builderService.equalsDbms()) {
                rawSql = Optional.ofNullable(builderService.makeSql(true)).orElse("");
                log.info("raw SQL (DBMS matches): {}", rawSql);
            } else {
                for (BizQueryMetaVO bqmvo : builderService.getBizQueryMetaList()) {
                    // 1. 개별 타겟 ID 및 테이블명 생성
                    String outTableId = "%s_%d".formatted(audienceId, bqmvo.getSeq());
                    bqmvo.setOutTableId(outTableId);

                    // 2. SQL 정제 (Java 21 String 메서드 및 정규식 활용)
                    rawSql = Optional.ofNullable(bqmvo.getQueryMeta()).orElse("");
                    String inSql = WHITESPACE_PATTERN.matcher(LINE_BREAK_PATTERN.matcher(rawSql).replaceAll(" "))
                            .replaceAll(" ");
                    inSql = OP_PATTERN.matcher(inSql).replaceAll("MULTIOP");

                    // 3. Source DB 정보 추출
                    String inConnId = bqmvo.getDbmsId();

                    log.info("Transferring data: [Source: {}] -> [Target Table: {}]", inConnId, audienceId);

                    // 4. 서로 다른 DB 간 데이터 이체 실행 (Fetch & Batch Insert)
                    // 개별 Audience ID(A_1, A_2 등)로 T_AI_TARGET_RESULT에 적재

                    int rowCount = transferToResultTable(inConnId, outDs, sessionId, audienceId, inSql);

                    log.info("Targeting sequence {} completed. Rows: {}", bqmvo.getSeq(), rowCount);
                }

                rawSql = Optional.ofNullable(builderService.makeSql(false)).orElse("");
                log.info("raw SQL (DBMS differs): {}", rawSql);

            }

            String selectQuery = WHITESPACE_PATTERN.matcher(LINE_BREAK_PATTERN.matcher(rawSql).replaceAll(" "))
                    .replaceAll(" ");
            // 6. 실행 파라미터 구성
            String inConnId = bizQueryMetaList.get(0).getDbmsId();
            // 타겟 DB의 URL에서 Connection ID 추출 로직 필요

            // 1. 기존 동일 세션/타겟 데이터 삭제 (재실행 대비)
            String deleteSql = "DELETE FROM T_AI_TARGET_RESULT WHERE SESSION_ID = :sessionId AND AUDIENCE_ID = :audienceId";
            MapSqlParameterSource delParams = new MapSqlParameterSource().addValue("sessionId", sessionId)
                    .addValue("audienceId", audienceId);
            queryService.update(deleteSql, delParams);

            // 2. 결과 적재 (INSERT INTO ... SELECT)
            // 소스 DB(inConnId)와 타겟 DB(workDbId)가 다를 경우 transferRecords 로직을 활용해야 합니다.
            String insertSql = """
                    INSERT INTO T_AI_TARGET_RESULT (SESSION_ID, AUDIENCE_ID, CUST_ID, REG_DTM)
                    SELECT :sessionId, :audienceId, CUST_ID, NOW()
                    FROM (%s) AS T
                    """.formatted(selectQuery);

            // DB가 다를 경우: fetch하여 Batch Insert
            int rowCount = transferToResultTable(inConnId, outDs, sessionId, audienceId, selectQuery);

            // 3. 이력 테이블(HIST) 적재
            insertTargetExecHist(sessionId, audienceId, "T_AI_TARGET_RESULT", selectQuery, String.valueOf(rowCount));

            resultDto.setRowCnt(String.valueOf(rowCount));
            resultDto.setTargetSuccess(true);
            resultDto.setOutTableName("T_AI_TARGET_RESULT"); // 공용 테이블명 반환// 공용 테이블명 반환 공용 테이블명 반환

            log.info("Targeting Table Created: {} (Count: {})", outTableName, rowCount);

        } catch (Exception e) {
            log.error("Failed to execute target service: {}", e.getMessage(), e);
            resultDto.setTargetSuccess(false);
        }
        return resultDto;
    }

    /**
     * 데이터를 한 건씩 읽어서 공용 결과 테이블에 적재
     */
    private int transferToResultTable(String inConnId, javax.sql.DataSource outDs, String sid, String aid, String sql)
            throws Exception {

        String insertSql = "INSERT INTO T_AI_TARGET_RESULT (SESSION_ID, AUDIENCE_ID, CUST_ID, REG_DTM) VALUES (?, ?, ?, NOW())";
        int count = 0;
        int commitSize = 10000;
        HikariDataSource inDs = dataSourceConfig.getDataSource(inConnId);
        // Try-with-resources를 통한 자원 관리
        try (Connection inConn = inDs.getConnection();
                Connection outConn = outDs.getConnection();
                PreparedStatement psIn = inConn.prepareStatement(sql);
                ResultSet rs = psIn.executeQuery();
                PreparedStatement psOut = outConn.prepareStatement(insertSql)) {

            // 성능 최적화 설정
            outConn.setAutoCommit(false); // 일괄 커밋을 위해 수동 커밋 모드
            rs.setFetchSize(commitSize); // 소스 DB에서 읽어올 때 버퍼 크기 지정

            while (rs.next()) {
                psOut.setString(1, sid);
                psOut.setString(2, aid);
                psOut.setString(3, rs.getString(1)); // SELECT 결과의 첫 번째 컬럼(CUST_ID)
                psOut.addBatch();

                count++;

                // commitSize(1000건)마다 배치 실행 및 커밋
                if (count % commitSize == 0) {
                    psOut.executeBatch();
                    outConn.commit();
                    psOut.clearBatch();
                    log.debug("[{}] {}건 데이터 이전 중...", aid, count);
                }
            }

            // 4. 남은 잔여 데이터 처리
            if (count % commitSize != 0) {
                psOut.executeBatch();
                outConn.commit();
            }

            log.info("Targeting transfer completed. Audience: {}, Total Count: {}", aid, count);

        } catch (Exception e) {
            log.error("Data transfer failed between {} and {}", inConnId, outDs.getConnection(), e);
            throw e; // 상위 호출부(getBizTargetService)에서 예외 처리하도록 던짐
        }

        return count;
    }

    private void insertTargetExecHist(String sessionId, String audienceId, String tableName, String query,
            String rowCnt) {
        // 테이블명 변경: T_AI_TARGET_EXEC_HIST
        String histSql = """
                INSERT INTO T_AI_TARGET_EXEC_HIST (
                    EXEC_ID, SESSION_ID, AUDIENCE_ID, OUT_TABLE_NAME,
                    QUERY_TEXT, ROW_CNT, TARGET_STATUS, STRT_DTM
                ) VALUES (
                    :execId, :sessionId, :audienceId, :tableName,
                    :query, :rowCnt, 'SUCCESS', NOW()
                )
                """;

        // 실행 ID 생성 (EXE_시분초밀리세컨드)
        String execId = "EXE_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        MapSqlParameterSource histParams = new MapSqlParameterSource().addValue("execId", execId)
                .addValue("sessionId", sessionId).addValue("audienceId", audienceId).addValue("tableName", tableName)
                .addValue("query", query).addValue("rowCnt", Long.parseLong(rowCnt));

        try {
            // 관리용 DB(워크 DB)에 이력 저장
            queryService.update(histSql, histParams);
            log.info("AI Target History recorded: {}", execId);
        } catch (Exception e) {
            log.error("Failed to record AI Target History: {}", e.getMessage());
        }
    }

    public BizTargetResultDto processCombinationToResult(String sessionId, AudienceDto audienceDto) {
        // 1. 재귀적으로 집합 연산 SQL 생성
        String combinationSql = buildSetOpResultSql(sessionId, audienceDto);
        if (combinationSql.isBlank()) {
            log.warn("Combination SQL is empty for sessionId: {}", sessionId);
            return BizTargetResultDto.builder().sessionId(sessionId).audienceId("").rowCnt("0").targetSuccess(false)
                    .query("").outTableName("").build();
        }
        log.info("Generated Combination SQL: {}", combinationSql);

        // 최종 적재할 Audience ID 지정
        String finalAudienceId = "Result";

        // 2. INSERT INTO ... SELECT 쿼리 조립
        String insertSql = """
                INSERT INTO T_AI_TARGET_RESULT (SESSION_ID, AUDIENCE_ID, CUST_ID, REG_DTM)
                SELECT :sessionId, :finalAudienceId, CUST_ID, NOW()
                FROM (%s) AS COMBINED_FINAL
                """.formatted(combinationSql);

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("sessionId", sessionId)
                .addValue("finalAudienceId", finalAudienceId);

        try {
            // [중복 방지] 기존에 생성된 'Result' 데이터가 있다면 먼저 삭제
            queryService.update(
                    "DELETE FROM T_AI_TARGET_RESULT WHERE SESSION_ID = :sessionId AND AUDIENCE_ID = :finalAudienceId",
                    params);

            // [데이터 적재] 집합 연산 결과 밀어넣기
            int totalRows = queryService.update(insertSql, params);

            // 3. 실행 이력(HIST) 기록
            insertTargetExecHist(sessionId, finalAudienceId, "T_AI_TARGET_RESULT", combinationSql,
                    String.valueOf(totalRows));

            log.info("Final targeting result 'Result' recorded. Count: {}", totalRows);

            return BizTargetResultDto.builder().sessionId(sessionId).audienceId(finalAudienceId)
                    .rowCnt(String.valueOf(totalRows)).targetSuccess(true).query(combinationSql)
                    .outTableName("T_AI_TARGET_RESULT").build();

        } catch (Exception e) {
            log.error("Failed to record final combination result: {}", e.getMessage(), e);
            throw new RuntimeException("최종 결과 적재 실패", e);
        }
    }

}
