package com.company.orchestrator.exeption;

import com.company.orchestrator.api.dto.ApiResponse;
import com.company.orchestrator.api.util.ResponseEntityBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST APIs
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorDetails>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        String errorMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        ValidationErrorDetails details = ValidationErrorDetails.builder()
            .fieldErrors(fieldErrors)
            .errorCount(fieldErrors.size())
            .build();

        ApiResponse<ValidationErrorDetails> response = ApiResponse.<ValidationErrorDetails>builder()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .message("Validation failed: " + errorMessage)
            .data(details)
            .success(false)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorDetails details = ErrorDetails.builder()
            .errorType("IllegalArgumentException")
            .errorDetails(ex.getMessage())
            .build();

        ApiResponse<ErrorDetails> response = ApiResponse.<ErrorDetails>builder()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .message(ex.getMessage())
            .data(details)
            .success(false)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(CancelTransferException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> cancelTransferException(CancelTransferException ex) {
        log.warn("CancelTransferException: {}", ex.getMessage());

        ErrorDetails details = ErrorDetails.builder()
                                           .errorType("CancelTransferException")
                                           .errorDetails(ex.getMessage())
                                           .build();

        ApiResponse<ErrorDetails> response = ApiResponse.<ErrorDetails>builder()
                                                        .statusCode(HttpStatus.EXPECTATION_FAILED.value())
                                                        .message(ex.getMessage())
                                                        .data(details)
                                                        .success(false)
                                                        .timestamp(LocalDateTime.now())
                                                        .build();

        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorDetails details = ErrorDetails.builder()
            .errorType(ex.getClass().getSimpleName())
            .errorDetails(ex.getMessage())
            .build();

        ApiResponse<ErrorDetails> response = ApiResponse.<ErrorDetails>builder()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("Internal server error occurred")
            .data(details)
            .success(false)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        // Silently ignore favicon requests
        if (ex.getMessage().contains("favicon.ico")) {
            return ResponseEntity.notFound().build();
        }

        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntityBuilder.notFound(ex.getMessage());
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationErrorDetails {
        private Map<String, String> fieldErrors;
        private int errorCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorDetails {
        private String errorType;
        private String errorDetails;
    }
}

