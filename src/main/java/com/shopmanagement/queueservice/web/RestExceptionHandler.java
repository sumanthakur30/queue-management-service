package com.shopmanagement.queueservice.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shopmanagement.queueservice.support.SlotAlreadyBookedException;
import com.sugamflow.observability.error.ErrorCodes;
import com.sugamflow.observability.error.ErrorEnvelope;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCodes.VALIDATION);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> forbidden(SecurityException ex) {
        return body(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<Map<String, Object>> slotConflict(SlotAlreadyBookedException ex) {
        return body(HttpStatus.CONFLICT,
                ex.getMessage() != null ? ex.getMessage() : SlotAlreadyBookedException.MESSAGE,
                ErrorCodes.CONFLICT);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> illegalState(IllegalStateException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCodes.QUEUE);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, String errorCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        ErrorEnvelope.apply(body, errorCode != null ? errorCode : ErrorCodes.VALIDATION);
        if (errorCode == null) {
            body.remove("errorCode");
        }
        return ResponseEntity.status(status).headers(ErrorEnvelope.headers()).body(body);
    }
}
