package com.example.bitbucketstats.exceptions;

import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  private static String nowIso() {
    return OffsetDateTime.now().toString();
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<ErrorResponse> handleWebExchangeBind(
      WebExchangeBindException ex, ServerHttpRequest request) {

    var violations = new ArrayList<>(ex.getFieldErrors().stream()
        .map(fe -> new ErrorResponse.Violation(fe.getField(), fe.getDefaultMessage()))
        .toList());

    ex.getGlobalErrors().forEach(ge ->
        violations.add(new ErrorResponse.Violation(ge.getObjectName(), ge.getDefaultMessage())));

    var body = new ErrorResponse(
        nowIso(),
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.toString(),
        "Validation failed",
        request.getURI().getPath(),
        violations
    );
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, ServerHttpRequest request) {

    var violations = ex.getConstraintViolations().stream()
        .map(v -> new ErrorResponse.Violation(v.getPropertyPath().toString(), v.getMessage()))
        .toList();

    var body = new ErrorResponse(
        nowIso(),
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.toString(),
        "Validation failed",
        request.getURI().getPath(),
        violations
    );
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(WebClientResponseException.class)
  public ResponseEntity<ErrorResponse> handleWebClientResponse(
      WebClientResponseException ex, ServerHttpRequest request) {
    log.debug("WebClient error {} on {}: {}", ex.getStatusCode(), request.getURI(), ex.getMessage());

    var body = new ErrorResponse(
        nowIso(),
        ex.getStatusCode().value(),
        ex.getStatusCode().toString(),
        // Prefer server-provided body if present; fallback to exception message
        Optional.of(ex.getResponseBodyAsString()).filter(s -> !s.isBlank()).orElse(ex.getMessage()),
        request.getURI().getPath(),
        null
    );
    return ResponseEntity.status(ex.getStatusCode()).body(body);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, ServerHttpRequest request) {
    var status = ex.getStatusCode();
    var body = new ErrorResponse(
        nowIso(),
        status.value(),
        status.toString(),
        Optional.ofNullable(ex.getReason()).orElse(ex.getMessage()),
        request.getURI().getPath(),
        null
    );
    return ResponseEntity.status(status).body(body);
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<ErrorResponse> handleAny(Throwable ex, ServerHttpRequest request) {
    log.error("Unhandled error on {}: {}", request.getURI(), ex.toString(), ex);

    var body = new ErrorResponse(
        nowIso(),
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.toString(),
        ex.getMessage(),
        request.getURI().getPath(),
        null
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}

