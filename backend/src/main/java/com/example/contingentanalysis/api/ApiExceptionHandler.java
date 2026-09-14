package com.example.contingentanalysis.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private String getRequestId() {
        String reqId = MDC.get("requestId");
        return reqId != null ? reqId : "N/A";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        String detail = errors.isEmpty() ? "Validation failed" : String.join("; ", errors);
        String reqId = getRequestId();
        log.warn("Validation failure [reqId={} path={}]: {}", reqId, request.getRequestURI(), detail);

        ApiErrorResponse body = ApiErrorResponse.of(detail, HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), reqId, errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String reqId = getRequestId();
        log.warn("Malformed JSON payload [reqId={} path={}]: {}", reqId, request.getRequestURI(), ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of("Malformed JSON request payload.", HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), reqId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        String reqId = getRequestId();
        String message = ex.getMessage() != null ? ex.getMessage() : "Bad request";
        log.warn("Bad request [reqId={} path={}]: {}", reqId, request.getRequestURI(), message);
        ApiErrorResponse body = ApiErrorResponse.of(message, HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), reqId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String reqId = getRequestId();
        log.warn("Max upload size exceeded [reqId={} path={}]: {}", reqId, request.getRequestURI(), ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of("Uploaded file exceeds the maximum permitted size of 15MB.", HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), reqId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiErrorResponse> handleMissingParams(Exception ex, HttpServletRequest request) {
        String reqId = getRequestId();
        log.warn("Missing request parameter or part [reqId={} path={}]: {}", reqId, request.getRequestURI(), ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), reqId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        String reqId = getRequestId();
        log.warn("Resource not found [reqId={} path={}]: {}", reqId, request.getRequestURI(), ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of("Resource not found: " + request.getRequestURI(), HttpStatus.NOT_FOUND.value(), request.getRequestURI(), reqId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        String reqId = getRequestId();
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        log.warn("Response status exception [reqId={} path={} status={}]: {}", reqId, request.getRequestURI(), ex.getStatusCode(), reason);
        ApiErrorResponse body = ApiErrorResponse.of(reason, ex.getStatusCode().value(), request.getRequestURI(), reqId);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String reqId = getRequestId();
        log.error("Unhandled exception [reqId={} path={}]: {}", reqId, request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse body = ApiErrorResponse.of("An unexpected internal server error occurred. Reference request ID: " + reqId, HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getRequestURI(), reqId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
