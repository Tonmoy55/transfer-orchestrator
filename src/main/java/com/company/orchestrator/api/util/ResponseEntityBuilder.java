package com.company.orchestrator.api.util;

import com.company.orchestrator.api.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for building standardized ResponseEntity objects
 */
public class ResponseEntityBuilder {

    private ResponseEntityBuilder() {
        // Utility class
    }

    /**
     * Creates a successful response with data
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.success(data, message, status.value()));
    }

    /**
     * Creates a successful OK (200) response
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return success(data, "Success", HttpStatus.OK);
    }

    /**
     * Creates a successful OK (200) response with custom message
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return success(data, message, HttpStatus.OK);
    }

    /**
     * Creates a successful CREATED (201) response
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return success(data, "Resource created successfully", HttpStatus.CREATED);
    }

    /**
     * Creates a successful CREATED (201) response with custom message
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return success(data, message, HttpStatus.CREATED);
    }

    /**
     * Creates a successful ACCEPTED (202) response
     */
    public static <T> ResponseEntity<ApiResponse<T>> accepted(T data) {
        return success(data, "Request accepted", HttpStatus.ACCEPTED);
    }

    /**
     * Creates a successful ACCEPTED (202) response with custom message
     */
    public static <T> ResponseEntity<ApiResponse<T>> accepted(T data, String message) {
        return success(data, message, HttpStatus.ACCEPTED);
    }

    /**
     * Creates a NO_CONTENT (204) response
     */
    public static <T> ResponseEntity<ApiResponse<T>> noContent() {
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, "No content", HttpStatus.NO_CONTENT.value()));
    }

    /**
     * Creates a BAD_REQUEST (400) error response
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Creates a BAD_REQUEST (400) error response with data
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(T data, String message) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(data, message, HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Creates a NOT_FOUND (404) error response
     */
    public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message, HttpStatus.NOT_FOUND.value()));
    }

    /**
     * Creates an INTERNAL_SERVER_ERROR (500) error response
     */
    public static <T> ResponseEntity<ApiResponse<T>> internalServerError(String message) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    /**
     * Creates a custom error response
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(message, status.value()));
    }

    /**
     * Creates a custom error response with data
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(T data, String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(data, message, status.value()));
    }
}

