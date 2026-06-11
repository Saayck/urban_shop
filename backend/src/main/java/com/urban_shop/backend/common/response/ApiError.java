package com.urban_shop.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    int status,
    String error,
    String message,
    String path,
    LocalDateTime timestamp,
    Map<String, String> fieldErrors
) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, path, LocalDateTime.now(), null);
    }

    public static ApiError validation(String path, Map<String, String> fieldErrors) {
        return new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Validation failed",
            path,
            LocalDateTime.now(),
            fieldErrors
        );
    }
}
