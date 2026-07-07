package com.azthera.ecocore.util;
 
import java.util.function.Consumer;
import java.util.function.Function;
 
/**
 * Generic success/failure wrapper used across service layers to avoid throwing
 * exceptions for expected business failures (e.g. insufficient funds, item out of stock).
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {
 
    record Success<T>(T value) implements Result<T> {}
 
    record Failure<T>(String message, Throwable cause) implements Result<T> {}
 
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }
 
    static <T> Result<T> failure(String message) {
        return new Failure<>(message, null);
    }
 
    static <T> Result<T> failure(String message, Throwable cause) {
        return new Failure<>(message, cause);
    }
 
    default boolean isSuccess() {
        return this instanceof Success<T>;
    }
 
    default boolean isFailure() {
        return this instanceof Failure<T>;
    }
 
    default T orElse(T fallback) {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> fallback;
        };
    }
 
    default <R> Result<R> map(Function<T, R> mapper) {
        return switch (this) {
            case Success<T> s -> Result.success(mapper.apply(s.value()));
            case Failure<T> f -> Result.failure(f.message(), f.cause());
        };
    }
 
    default Result<T> onSuccess(Consumer<T> consumer) {
        if (this instanceof Success<T> s) {
            consumer.accept(s.value());
        }
        return this;
    }
 
    default Result<T> onFailure(Consumer<String> consumer) {
        if (this instanceof Failure<T> f) {
            consumer.accept(f.message());
        }
        return this;
    }
}
