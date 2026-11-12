package com.example.HealthCare.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status.value());
        map.put("message", message);
        return ResponseEntity.status(status).body(map);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return body(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException ex) {
        return body(HttpStatus.BAD_REQUEST, "Requête invalide : vérifiez les données envoyées");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex) {
        return body(HttpStatus.CONFLICT, "Opération impossible : des données liées existent (ou valeur déjà utilisée)");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return body(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, ex.getReason());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> runtime(RuntimeException ex) {
        // let Spring Security handle its own exceptions (401 / 403)
        if (ex instanceof AccessDeniedException) throw (AccessDeniedException) ex;
        if (ex instanceof AuthenticationException) throw (AuthenticationException) ex;

        String message = ex.getMessage() == null ? "Unexpected error" : ex.getMessage();
        String lower = message.toLowerCase();
        if (lower.contains("not found")) return body(HttpStatus.NOT_FOUND, message);
        if (lower.contains("already")) return body(HttpStatus.CONFLICT, message);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}