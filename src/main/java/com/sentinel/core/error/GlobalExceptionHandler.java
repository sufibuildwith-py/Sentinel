package com.sentinel.core.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import com.sentinel.revenue.webhook.InvalidWebhookSignatureException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed",
                path(request), violations);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is malformed",
                path(request), List.of());
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    ResponseEntity<Object> handleUpstreamTimeout(UpstreamTimeoutException exception, HttpServletRequest request) {
        log.warn("Upstream request timed out: {}", exception.getService());
        return response(HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT",
                "An upstream service timed out", request.getRequestURI(), List.of());
    }

    @ExceptionHandler({UpstreamServiceException.class, InvalidModelResponseException.class})
    ResponseEntity<Object> handleUpstreamFailure(RuntimeException exception, HttpServletRequest request) {
        log.warn("Upstream dependency failed: {}", exception.getMessage());
        return response(HttpStatus.BAD_GATEWAY, "UPSTREAM_FAILURE",
                "An upstream service could not complete the investigation", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", exception.getMessage(),
                request.getRequestURI(), List.of());
    }

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    ResponseEntity<Object> handleInvalidWebhookSignature(InvalidWebhookSignatureException exception,
                                                          HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE",
                "Webhook signature validation failed", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Object> handleIllegalState(IllegalStateException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "STATE_CONFLICT", exception.getMessage(),
                request.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected request failure", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed", request.getRequestURI(), List.of());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());
        HttpStatus httpStatus = resolvedStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolvedStatus;
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                httpStatus.getReasonPhrase(),
                "HTTP_REQUEST_ERROR",
                "The HTTP request could not be processed",
                path(request),
                List.of()
        );
        return ResponseEntity.status(status).headers(headers).body(error);
    }

    private ResponseEntity<Object> response(
            HttpStatus status,
            String code,
            String message,
            String path,
            List<FieldViolation> violations
    ) {
        ApiError error = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), code,
                message, path, violations);
        return ResponseEntity.status(status).body(error);
    }

    private String path(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "";
    }
}
