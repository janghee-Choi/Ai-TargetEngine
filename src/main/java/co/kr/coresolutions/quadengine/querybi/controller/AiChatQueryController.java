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

import co.kr.coresolutions.quadengine.querybi.dto.AiChatQueryRequestDto;
import co.kr.coresolutions.quadengine.querybi.dto.AiChatQueryResponseDto;
import co.kr.coresolutions.quadengine.querybi.dto.AiChatResultDto;
import co.kr.coresolutions.quadengine.querybi.dto.AudienceDetailDto;
import co.kr.coresolutions.quadengine.querybi.dto.AudienceDto;
import co.kr.coresolutions.quadengine.querybi.dto.BizTargetResultDto;
import co.kr.coresolutions.quadengine.querybi.dto.ErrorResponseDto.FieldError;
import co.kr.coresolutions.quadengine.querybi.dto.TMetaDto;
import co.kr.coresolutions.quadengine.querybi.dto.TMetaResultDto;
import co.kr.coresolutions.quadengine.querybi.service.AiChatResultService;
import co.kr.coresolutions.quadengine.querybi.service.BizTargetService;

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

    private final ObjectMapper objectMapper;

    @PostMapping("/audience/targeting")
    public ResponseEntity<AiChatQueryResponseDto> audienceTargeting(
            @Validated @RequestBody AiChatQueryRequestDto request) {
        log.info("Audience Targeting API 호출 - 요청 데이터: {}", request);

        String sessionId = request.getSessionId();

        List<AiChatResultDto> aiChatResultList = aiChatResultService.getResultsBySessionId(sessionId);

        AudienceDto audienceDto = aiChatResultList.stream()
                .filter(aiChatResult -> aiChatResult.getKeyId().equals("audience")).map(aiChatResult -> {
                    try {
                        return objectMapper.readValue(aiChatResult.getResult(), AudienceDto.class);
                    } catch (Exception e) {
                        log.error("Failed to parse AudienceDto from JSON: {}", aiChatResult.getResult(), e);
                        return null;
                    }
                }).findFirst().orElse(null);

        if (audienceDto == null) {
            log.warn("No AudienceDto found for sessionId: {}", sessionId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AiChatQueryResponseDto(false,
                    "No Audience found for sessionId: " + sessionId, null, null, null, null));
        }

        // TMetaDto 리스트 추출 및 변환
        List<TMetaDto> tMetaDtoList = aiChatResultList.stream()
                .filter(aiChatResult -> aiChatResult.getKeyId().equals("tmeta")).map(aiChatResult -> {
                    try {

                        // JSON 배열을 List<TMetaResultDto>로 역직렬화
                        List<TMetaResultDto> metaResults = objectMapper.readValue(aiChatResult.getResult(),
                                new TypeReference<List<TMetaResultDto>>() {
                                });

                        TMetaDto tMetaDto = new TMetaDto();
                        tMetaDto.setSessionId(aiChatResult.getSessionId());
                        tMetaDto.setAudienceId(aiChatResult.getAudienceId());

                        tMetaDto.setTMetaResultList(metaResults);
                        return tMetaDto;
                    } catch (Exception e) {
                        log.error("Failed to parse TMetaDto from JSON: {}", aiChatResult.getResult(), e);
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());

        // TMetaDto가 하나도 없는 경우 에러 처리
        if (tMetaDtoList.isEmpty()) {
            log.warn("No TMetaDto found for sessionId: {}", sessionId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AiChatQueryResponseDto(false,
                    "No TMetaDto found for sessionId: " + sessionId, null, null, null, null));
        }

        // TMetaDto 중 하나라도 success가 false인 경우 전체 실패로 간주
        if (tMetaDtoList.stream()
                .anyMatch(tMeta -> !tMeta.getTMetaResultList().stream().allMatch(TMetaResultDto::isSuccess))) {
            log.warn("One or more TMetaDto entries indicate failure for sessionId: {}", sessionId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AiChatQueryResponseDto(false,
                    "One or more TMeta entries indicate failure for sessionId: " + sessionId, null, null, null, null));
        }

        try {

            String tMetaDtoJson = objectMapper.writeValueAsString(tMetaDtoList);
            log.info("Converted TMetaDtoList to JSON: {}", tMetaDtoJson);
        } catch (Exception e) {
            log.error("Failed to convert TMetaDtoList to JSON", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AiChatQueryResponseDto(false,
                    "Failed to convert TMetaDtoList to JSON : " + e.getLocalizedMessage(), null, null, null, null));
        }

        // queryId 기준으로 TMetaResultDto 그룹핑

        Map<String, Map<String, List<TMetaResultDto>>> groupedByAudience = tMetaDtoList.stream()
                .collect(Collectors.toMap(TMetaDto::getAudienceId, dto -> dto.getTMetaResultList().stream()
                        .collect(Collectors.groupingBy(TMetaResultDto::getQueryId))));
        // queryId 기준 쿼리 생성

        try {
            List<BizTargetResultDto> finalResults = groupedByAudience.entrySet().stream().map(entry -> {
                String audienceId = entry.getKey();
                Map<String, List<TMetaResultDto>> queryGroupMap = entry.getValue();
                // sessionId는 DTO나 별도 파라미터에서 추출
                return bizTargetService.getBizTargetService(sessionId, audienceId, queryGroupMap);
            }).toList();
            String finalResultsJson = objectMapper.writeValueAsString(finalResults);
            log.info("Final BizTargetResultDto List (JSON): {}", finalResultsJson);
        } catch (Exception e) {
            log.error("Failed to convert tMetaGroupedDtoList to JSON", e);
        }

        BizTargetResultDto finalResult = bizTargetService.processCombinationToResult(sessionId, audienceDto);

        log.info("Generated Final Result: {}", finalResult);

        log.info("Retrieved AudienceDto: {}", audienceDto);
        log.info("Retrieved TMetaDtoList: {}", tMetaDtoList);

        log.info("Audience Targeting API 호출 - 성공적으로 처리됨");
        AiChatQueryResponseDto responseBody = AiChatQueryResponseDto.builder().success(finalResult.isTargetSuccess())
                .message("Audience targeting completed successfully").count(finalResult.getRowCnt())
                .query(finalResult.getQuery()).Audience(audienceDto).setOperation(audienceDto.getSetOperation())
                .build();

        return ResponseEntity.ok(responseBody);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AiChatQueryResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        // 1. 모든 필드 에러 메시지를 수집하여 콤마(,)로 결합
        String errorMessage = ex.getBindingResult().getFieldErrors().stream().map(m -> m.getDefaultMessage()) // FieldError
                .filter(Objects::nonNull) // Null 방어 (Optional)
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: [{}]", errorMessage);

        // 2. Builder 패턴으로 응답 객체 생성
        AiChatQueryResponseDto response = AiChatQueryResponseDto.builder().success(false)
                .message("Invalid request parameters: " + errorMessage).build();

        return ResponseEntity.badRequest().body(response);
    }
}
