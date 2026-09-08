package com.example.support.api;

import java.util.Optional;

/**
 * Transport-neutral result of a controller call: a status code, an optional
 * payload and, on failure, an error message.
 */
public record ApiResponse<T>(int statusCode, T body, String errorMessage) {

    public static <T> ApiResponse<T> ok(T body) {
        return new ApiResponse<>(200, body, null);
    }

    public static <T> ApiResponse<T> created(T body) {
        return new ApiResponse<>(201, body, null);
    }

    public static <T> ApiResponse<T> noContent() {
        return new ApiResponse<>(204, null, null);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, null, message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, null, message);
    }

    public static <T> ApiResponse<T> conflict(String message) {
        return new ApiResponse<>(409, null, message);
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public Optional<T> bodyAsOptional() {
        return Optional.ofNullable(body);
    }
}
