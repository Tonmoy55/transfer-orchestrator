package com.company.orchestrator.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API Response wrapper for all endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * HTTP status code
     */
    private int statusCode;

    /**
     * Response message
     */
    private String message;

    /**
     * Response data (payload)
     */
    private T data;

    /**
     * Timestamp of the response
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Success indicator
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Creates a successful response with data
     */
    public static <T> ApiResponse<T> success(T data, String message, int statusCode) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a successful response with data and default message
     */
    public static <T> ApiResponse<T> success(T data, int statusCode) {
        return success(data, "Success", statusCode);
    }

    /**
     * Creates a successful response with default 200 status
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Success", 200);
    }

    /**
     * Creates an error response
     */
    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(null)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with data
     */
    public static <T> ApiResponse<T> error(T data, String message, int statusCode) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

