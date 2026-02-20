package com.sw.signalFlowBackend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SymbolNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSymbolNotFound(SymbolNotFoundException ex){
        return createErrorResponse(ex.getMessage(), 404);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return createErrorResponse(ex.getMessage(), 400);
    }

    // Generic Helper
    private ResponseEntity<Map<String, String>> createErrorResponse(String msg, int status) {
        Map<String, String> response = new HashMap<>();
        response.put("error", msg);
        response.put("status", String.valueOf(status));
        return ResponseEntity.status(status).body(response);
    }
}
