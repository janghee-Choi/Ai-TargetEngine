package co.kr.coresolutions.quadengine.querybi.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.kr.coresolutions.quadengine.querybi.interfaces.KeyedResult;
import co.kr.coresolutions.quadengine.querybi.interfaces.ParseResult;
import co.kr.coresolutions.quadengine.querybi.exception.AiTargetingException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JsonParseService {

    private final ObjectMapper objectMapper;

    // 단건 파싱
    public <T> ParseResult<T> parse(String json, Class<T> clazz) {
        try {
            return ParseResult.success(objectMapper.readValue(json, clazz), json);
        } catch (JsonProcessingException e) {
            return ParseResult.failure(json, e);
        }
    }

    // TypeReference 지원 (List<T>, Map<K,V> 등)
    public <T> ParseResult<T> parse(String json, TypeReference<T> typeRef) {
        try {
            return ParseResult.success(objectMapper.readValue(json, typeRef), json);
        } catch (JsonProcessingException e) {
            return ParseResult.failure(json, e);
        }
    }

    // 스트림에서 keyId로 필터링 후 파싱
    public <T> ParseResult<T> parseFromList(List<? extends KeyedResult> list, String keyId, Class<T> clazz) {
        return list.stream().filter(r -> keyId.equals(r.getKeyId())).findFirst().map(r -> parse(r.getResult(), clazz))
                .orElse(ParseResult.empty());
    }

    // 스트림에서 여러 keyId 한번에 파싱
    public <T> Map<String, ParseResult<T>> parseFromListByKeys(List<? extends KeyedResult> list, List<String> keyIds,
            Class<T> clazz) {
        Map<String, String> jsonByKey = list.stream().filter(r -> keyIds.contains(r.getKeyId()))
                .collect(Collectors.toMap(KeyedResult::getKeyId, KeyedResult::getResult));

        return keyIds.stream().collect(Collectors.toMap(key -> key,
                key -> jsonByKey.containsKey(key) ? parse(jsonByKey.get(key), clazz) : ParseResult.empty()));
    }

    public <T> ParseResult<String> toJson(T object) {
        try {
            return ParseResult.success(objectMapper.writeValueAsString(object), null);
        } catch (JsonProcessingException e) {
            return ParseResult.failure(null, e);
        }
    }

}