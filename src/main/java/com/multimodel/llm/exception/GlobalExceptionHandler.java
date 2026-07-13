package com.multimodel.llm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler translating application exceptions into appropriate HTTP responses
 * across all {@code @RestController}s.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link InvalidAnswerException} by returning its message with a 400 Bad Request
     * status.
     *
     * @param ex the exception raised when a model answer fails fact-checking evaluation
     * @return a 400 Bad Request response containing the exception's message
     */
    @ExceptionHandler(InvalidAnswerException.class)
    public ResponseEntity<String> handleInvalidAnswer(InvalidAnswerException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
