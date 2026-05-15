package com.hireconnect.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * STEP 3 — GLOBAL EXCEPTION HANDLER
 *
 * Without this class:
 * - A missing notification returns an ugly HTML Spring error page
 * - A bad request body returns a wall of JSON with internal stack traces
 *
 * With this class:
 * ✅ Every error returns a clean, consistent JSON object
 * ✅ Frontend developers can reliably parse and display these errors
 *
 * How it works:
 * @RestControllerAdvice watches ALL controllers.
 * When any controller throws an exception, Spring checks here first
 * before returning the default error response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles: GET/PATCH/DELETE on a notification ID that doesn't exist
     * Returns: HTTP 404 with a clear message
     */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotificationNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 404);
        error.put("error", "Not Found");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles: POST /send with missing or invalid fields (@NotBlank, @Email violations)
     * Returns: HTTP 400 listing EVERY validation error so the caller knows exactly what to fix
     *
     * Example response:
     * {
     *   "status": 400,
     *   "errors": {
     *     "recipientEmail": "must be a valid email address",
     *     "message": "must not be blank"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // Collect all field-level validation errors into a map
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing   // keep first message if duplicate field
                ));

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 400);
        error.put("error", "Validation Failed");
        error.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Catches anything else (unexpected runtime errors)
     * Returns: HTTP 500 with a generic safe message (never expose stack traces to clients)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 500);
        error.put("error", "Internal Server Error");
        error.put("message", "Something went wrong. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}