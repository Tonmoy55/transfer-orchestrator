package com.company.orchestrator.api;

import com.company.orchestrator.api.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiResponse
 */
class ApiResponseTest {

    @Test
    @DisplayName("Should create success response with data")
    void shouldCreateSuccessResponseWithData() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data, "Success message", 200);

        assertEquals(200, response.getStatusCode());
        assertEquals("Success message", response.getMessage());
        assertEquals(data, response.getData());
        assertTrue(response.isSuccess());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @DisplayName("Should create success response with default message")
    void shouldCreateSuccessResponseWithDefaultMessage() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data);

        assertEquals(200, response.getStatusCode());
        assertEquals("Success", response.getMessage());
        assertEquals(data, response.getData());
        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("Should create error response")
    void shouldCreateErrorResponse() {
        ApiResponse<String> response = ApiResponse.error("Error message", 400);

        assertEquals(400, response.getStatusCode());
        assertEquals("Error message", response.getMessage());
        assertNull(response.getData());
        assertFalse(response.isSuccess());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @DisplayName("Should create error response with data")
    void shouldCreateErrorResponseWithData() {
        String errorData = "error details";
        ApiResponse<String> response = ApiResponse.error(errorData, "Error message", 500);

        assertEquals(500, response.getStatusCode());
        assertEquals("Error message", response.getMessage());
        assertEquals(errorData, response.getData());
        assertFalse(response.isSuccess());
    }

    @Test
    @DisplayName("Should have timestamp")
    void shouldHaveTimestamp() {
        LocalDateTime before = LocalDateTime.now();
        ApiResponse<String> response = ApiResponse.success("data");
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp().isAfter(before.minusSeconds(1)));
        assertTrue(response.getTimestamp().isBefore(after.plusSeconds(1)));
    }

    @Test
    @DisplayName("Should support null data")
    void shouldSupportNullData() {
        ApiResponse<String> response = ApiResponse.success(null, "Success with no data", 204);

        assertEquals(204, response.getStatusCode());
        assertNull(response.getData());
        assertTrue(response.isSuccess());
    }
}

