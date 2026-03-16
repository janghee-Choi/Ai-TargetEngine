package co.kr.coresolutions.quadengine.querybi.interfaces;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class ParseResult<T> {

    private final T value;
    private final String rawJson;
    private final JsonProcessingException error;
    private final ParseStatus status;

    public enum ParseStatus {
        SUCCESS, FAILURE, EMPTY
    }

    private ParseResult(T value, String rawJson, JsonProcessingException error, ParseStatus status) {
        this.value = value;
        this.rawJson = rawJson;
        this.error = error;
        this.status = status;
    }

    public static <T> ParseResult<T> success(T value, String rawJson) {
        return new ParseResult<>(value, rawJson, null, ParseStatus.SUCCESS);
    }

    public static <T> ParseResult<T> failure(String rawJson, JsonProcessingException error) {
        return new ParseResult<>(null, rawJson, error, ParseStatus.FAILURE);
    }

    public static <T> ParseResult<T> empty() {
        return new ParseResult<>(null, null, null, ParseStatus.EMPTY);
    }

    public boolean isSuccess() {
        return status == ParseStatus.SUCCESS;
    }

    public boolean isFailure() {
        return status == ParseStatus.FAILURE;
    }

    public boolean isEmpty() {
        return status == ParseStatus.EMPTY;
    }

    // 성공 시 값 처리
    public ParseResult<T> onSuccess(Consumer<T> action) {
        if (isSuccess())
            action.accept(value);
        return this;
    }

    // 실패 시 오류 처리
    public ParseResult<T> onFailure(BiConsumer<String, JsonProcessingException> action) {
        if (isFailure())
            action.accept(rawJson, error);
        return this;
    }

    // 비어있을 때 처리
    public ParseResult<T> onEmpty(Runnable action) {
        if (isEmpty())
            action.run();
        return this;
    }

    // 값 변환
    public <R> ParseResult<R> map(Function<T, R> mapper) {
        if (isSuccess())
            return ParseResult.success(mapper.apply(value), rawJson);
        if (isFailure())
            return ParseResult.failure(rawJson, error);
        return ParseResult.empty();
    }

    // 기본값 반환
    public T orElse(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    // 실패 시 예외 throw
    public T orElseThrow(Supplier<? extends RuntimeException> exSupplier) {
        if (isSuccess())
            return value;
        if (isFailure())
            throw exSupplier.get();
        throw exSupplier.get(); // EMPTY인 경우도 throw
    }

    // 에러 메시지 요약
    public String getErrorMessage() {
        return error != null ? error.getOriginalMessage() : null;
    }

    public String getErrorLocation() {
        if (error == null)
            return null;
        JsonLocation loc = error.getLocation();
        return loc != null ? "line %d, column %d".formatted(loc.getLineNr(), loc.getColumnNr()) : "unknown location";
    }
}