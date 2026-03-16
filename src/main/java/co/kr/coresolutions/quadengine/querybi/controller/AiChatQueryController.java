package co.kr.coresolutions.quadengine.querybi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.FieldError;

import co.kr.coresolutions.quadengine.querybi.dto.AiChatQueryRequestDto;
import co.kr.coresolutions.quadengine.querybi.dto.AiChatQueryResponseDto;
import co.kr.coresolutions.quadengine.querybi.dto.AiChatResultDto;
import co.kr.coresolutions.quadengine.querybi.dto.AudienceDetailDto;
import co.kr.coresolutions.quadengine.querybi.dto.AudienceDto;
import co.kr.coresolutions.quadengine.querybi.dto.BizTargetResultDto;
import co.kr.coresolutions.quadengine.querybi.dto.TMetaDto;
import co.kr.coresolutions.quadengine.querybi.dto.TMetaResultDto;
import co.kr.coresolutions.quadengine.querybi.enums.AiChatErrorCode;
import co.kr.coresolutions.quadengine.querybi.exception.AiTargetingException;
import co.kr.coresolutions.quadengine.querybi.interfaces.ParseResult;
import co.kr.coresolutions.quadengine.querybi.service.AiChatResultService;
import co.kr.coresolutions.quadengine.querybi.service.BizTargetService;
import co.kr.coresolutions.quadengine.querybi.service.JsonParseService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/querybi")
public class AiChatQueryController {

    private final AiChatResultService aiChatResultService;
    private final BizTargetService bizTargetService;
    private final JsonParseService jsonParseService;

    @PostMapping("/audience/targeting")
    public ResponseEntity<AiChatQueryResponseDto> audienceTargeting2(
            @Validated @RequestBody AiChatQueryRequestDto request) {
        log.info("Audience Targeting API 호출 - 요청 데이터: {}", request);

        String sessionId = request.getSessionId();
        List<AiChatResultDto> aiChatResultList = aiChatResultService.getResultsBySessionId(sessionId).onEmpty(() -> {
            log.warn("No results found for sessionId: {}", sessionId);
            throw new AiTargetingException(AiChatErrorCode.SESSION_NOT_FOUND, "Session not found - sessionId: {}",
                    sessionId);
        }).orElseThrow(() -> new AiTargetingException(AiChatErrorCode.SESSION_NOT_FOUND,
                "Session not found - sessionId: {}", sessionId));

        // ① AudienceDto 파싱
        AudienceDto audienceDto = jsonParseService.parseFromList(aiChatResultList, "audience", AudienceDto.class)
                .onFailure((json, e) -> {
                    log.error("Failed to parse AudienceDto: {}", json, e);
                    throw new AiTargetingException(AiChatErrorCode.AUDIENCE_PARSE_ERROR,
                            "Failed to parse AudienceDto - sessionId: {} , json: {}", sessionId, json);
                }).onEmpty(() -> {
                    log.warn("No AudienceDto found for sessionId: {}", sessionId);
                    throw new AiTargetingException(AiChatErrorCode.AUDIENCE_NOT_FOUND,
                            "No Audience found - sessionId: {} , json: {}", sessionId);
                }).orElseThrow(() -> new AiTargetingException(AiChatErrorCode.AUDIENCE_NOT_FOUND,
                        "No Audience found - sessionId: {} , json: {}", sessionId));

        // ② TMetaDto 리스트 파싱
        List<TMetaDto> tMetaDtoList = aiChatResultList.stream().filter(r -> "tmeta".equals(r.getKeyId())).map(r -> {
            ParseResult<TMetaDto> parseResult = jsonParseService
                    .parse(r.getResult(), new TypeReference<List<TMetaResultDto>>() {
                    }).onFailure((json, e) -> log.error("Failed to parse TMetaDto: {}", json, e))
                    .map(metaResults -> TMetaDto.builder().sessionId(r.getSessionId()).audienceId(r.getAudienceId())
                            .tMetaResultList(metaResults).build());
            return parseResult;
        }).filter(ParseResult::isSuccess).map(ParseResult::getValue).collect(Collectors.toList());

        // ③ TMetaDto 유효성 검증
        validateTMetaList(tMetaDtoList, sessionId);

        // ④ JSON 직렬화 로깅
        jsonParseService.toJson(tMetaDtoList).onSuccess(json -> log.info("Converted TMetaDtoList to JSON: {}", json))
                .onFailure((json, e) -> log.error("Failed to convert TMetaDtoList to JSON", e));

        // ⑤ AudienceId 기준 그룹핑 및 타겟 결과 생성
        Map<String, Map<String, List<TMetaResultDto>>> groupedByAudience = tMetaDtoList.stream()
                .collect(Collectors.toMap(TMetaDto::getAudienceId, dto -> dto.getTMetaResultList().stream()
                        .collect(Collectors.groupingBy(TMetaResultDto::getQueryId))));

        List<BizTargetResultDto> targetResults = groupedByAudience.entrySet().stream()
                .map(entry -> bizTargetService.getBizTargetService(sessionId, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // ⑥ 타겟 결과 로깅 및 유효성 검증
        logTargetResults(targetResults);
        validateTargetResults(targetResults, sessionId);

        // ⑦ 최종 결과
        BizTargetResultDto finalResult = bizTargetService.processCombinationToResult(sessionId, audienceDto);

        log.debug("Generated Final Result: {}", finalResult);
        log.debug("Retrieved AudienceDto: {}", audienceDto);
        log.debug("Retrieved TMetaDtoList: {}", tMetaDtoList);
        log.info("Audience Targeting API 호출 - 성공적으로 처리됨");

        return ResponseEntity.ok(AiChatQueryResponseDto.success(finalResult.getRowCnt(), finalResult.getQuery(),
                audienceDto, audienceDto.getSetOperation()));
    }

    private void validateTMetaList(List<TMetaDto> tMetaDtoList, String sessionId) {
        if (tMetaDtoList.isEmpty()) {
            log.warn("No TMetaDto found for sessionId: {}", sessionId);
            throw new AiTargetingException(AiChatErrorCode.TMETA_NOT_FOUND, "No TMetaDto found - sessionId: {}",
                    sessionId);
        }

        tMetaDtoList.stream().flatMap(tMeta -> tMeta.getTMetaResultList().stream()).filter(r -> !r.isSuccess())
                .findFirst().ifPresent(r -> {
                    log.warn("TMetaResultDto failure - sessionId: {}, queryId: {}, error: {}", sessionId,
                            r.getQueryId(), r.getErrorMessage());
                    throw new AiTargetingException(AiChatErrorCode.TMETA_FAILURE,
                            "TMeta failure - sessionId: {}, queryId: {}, error: {}", sessionId, r.getQueryId(),
                            r.getErrorMessage());
                });
    }

    private void validateTargetResults(List<BizTargetResultDto> targetResults, String sessionId) {
        if (targetResults == null || targetResults.isEmpty()) {
            log.warn("No targeting results generated for sessionId: {}", sessionId);
            throw new AiTargetingException(AiChatErrorCode.TARGETING_NOT_FOUND, "No targeting results - sessionId: {}",
                    sessionId);
        }

        targetResults.stream().filter(r -> !r.isTargetSuccess()).findFirst().ifPresent(r -> {
            log.warn("BizTarget failure - sessionId: {}, audienceId: {}, error: {}", sessionId, r.getAudienceId(),
                    r.getErrorMessage());
            throw new AiTargetingException(AiChatErrorCode.TARGETING_FAILED,
                    "Target failure - sessionId: {}, audienceId: {}, error: {}", sessionId, r.getAudienceId(),
                    r.getErrorMessage());
        });
    }

    private void logTargetResults(List<BizTargetResultDto> targetResults) {
        targetResults.forEach(result -> {
            if (result.isTargetSuccess()) {
                log.debug("Successful Targeting - AudienceId: {}, RowCount: {}, Query: {}", result.getAudienceId(),
                        result.getRowCnt(), result.getQuery());
            } else {
                log.error("Targeting failed - AudienceId: {}, Query: {}, Error: {}", result.getAudienceId(),
                        result.getQuery(), result.getErrorMessage());
            }
        });

        long successCount = targetResults.stream().filter(BizTargetResultDto::isTargetSuccess).count();
        log.info("Target results summary - total: {}, success: {}, failed: {}", targetResults.size(), successCount,
                targetResults.size() - successCount);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AiChatQueryResponseDto> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull).collect(Collectors.joining(", "));

        log.warn("Validation failed - uri: {}, message: [{}]", request.getRequestURI(), errorMessage);

        return ResponseEntity.badRequest()
                .body(AiChatQueryResponseDto.failure(String.valueOf(AiChatErrorCode.INVALID_REQUEST.getCode()),
                        "Invalid request parameters: " + errorMessage));
    }

    @ExceptionHandler(AiTargetingException.class)
    public ResponseEntity<AiChatQueryResponseDto> handleAiTargetingException(AiTargetingException e,
            HttpServletRequest request) {

        log.error("AiTargetingException - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getErrorCode(),
                e.getMessage());

        HttpStatus status = switch (e.getErrorCode()) {
        case SESSION_NOT_FOUND, AUDIENCE_NOT_FOUND, TMETA_NOT_FOUND, TARGETING_NOT_FOUND -> HttpStatus.NOT_FOUND; // 404
        case INVALID_REQUEST -> HttpStatus.BAD_REQUEST; // 400
        case AUDIENCE_PARSE_ERROR, TMETA_PARSE_ERROR -> HttpStatus.UNPROCESSABLE_ENTITY; // 422
        default -> HttpStatus.INTERNAL_SERVER_ERROR; // 500
        };

        return ResponseEntity.status(status)
                .body(AiChatQueryResponseDto.failure(String.valueOf(e.getErrorCode().getCode()), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AiChatQueryResponseDto> handleException(Exception e, HttpServletRequest request) {

        log.error("Unexpected error - uri: {}, message: {}", request.getRequestURI(), e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AiChatQueryResponseDto.failure(
                String.valueOf(AiChatErrorCode.INTERNAL_ERROR.getCode()), AiChatErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
