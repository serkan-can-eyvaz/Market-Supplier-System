package com.example.marketsupplier.exception;

import com.example.marketsupplier.util.LoggerUtility;
import com.example.marketsupplier.util.InputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private LoggerUtility loggerUtility;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("GLOBAL_EXCEPTION_HANDLER")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("exception_class", ex.getClass().getSimpleName());

        loggerUtility.logError("Unhandled exception occurred", context, ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred. Please try again later.")
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("VALIDATION_EXCEPTION")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("validation_errors", ex.getErrors());

        loggerUtility.logWarn("Validation exception occurred", context);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message("Input validation failed")
            .path(request.getDescription(false))
            .details(new HashMap<>(ex.getErrors()))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("SECURITY_EXCEPTION")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("security_violation", ex.getMessage());

        loggerUtility.logSecurity("Security violation detected", context, Map.of(
            "violation_type", ex.getClass().getSimpleName(),
            "message", ex.getMessage()
        ));

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Security Violation")
            .message("Access denied due to security policy violation")
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("BUSINESS_EXCEPTION")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("business_error_code", ex.getErrorCode());

        loggerUtility.logBusiness("Business exception occurred", context, Map.of(
            "error_code", ex.getErrorCode(),
            "message", ex.getMessage()
        ));

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Business Error")
            .message(ex.getMessage())
            .path(request.getDescription(false))
            .errorCode(ex.getErrorCode())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RESOURCE_NOT_FOUND")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("resource_type", ex.getResourceType())
            .withMetadata("resource_id", ex.getResourceId());

        loggerUtility.logWarn("Resource not found", context);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Resource Not Found")
            .message(ex.getMessage())
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(RateLimitExceededException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("RATE_LIMIT_EXCEEDED")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("retry_after", ex.getRetryAfterSeconds());

        loggerUtility.logWarn("Rate limit exceeded", context);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.TOO_MANY_REQUESTS.value())
            .error("Rate Limit Exceeded")
            .message(ex.getMessage())
            .path(request.getDescription(false))
            .retryAfter(ex.getRetryAfterSeconds())
            .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("HTTP_MESSAGE_NOT_READABLE")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false));

        loggerUtility.logWarn("Invalid JSON format", context);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Request Format")
            .message("Request body contains invalid JSON format")
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        Map<String, String> validationErrors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            validationErrors.put(error.getField(), error.getDefaultMessage())
        );

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("METHOD_ARGUMENT_NOT_VALID")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("validation_errors", validationErrors);

        loggerUtility.logWarn("Method argument validation failed", context);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message("Request parameters validation failed")
            .path(request.getDescription(false))
            .details(new HashMap<>(validationErrors))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("HTTP_METHOD_NOT_SUPPORTED")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("method", ex.getMethod());

        loggerUtility.logWarn("HTTP method not supported", context);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.METHOD_NOT_ALLOWED.value())
            .error("Method Not Allowed")
            .message("HTTP method not supported for this endpoint")
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("NO_HANDLER_FOUND")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("method", ex.getHttpMethod());

        loggerUtility.logWarn("No handler found for request", context);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message("Requested endpoint not found")
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("MISSING_REQUEST_PARAMETER")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("parameter_name", ex.getParameterName())
            .withMetadata("parameter_type", ex.getParameterType());

        loggerUtility.logWarn("Missing request parameter", context);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Missing Parameter")
            .message("Required request parameter is missing: " + ex.getParameterName())
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String errorId = UUID.randomUUID().toString();
        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("METHOD_ARGUMENT_TYPE_MISMATCH")
            .withMetadata("error_id", errorId)
            .withMetadata("request_uri", request.getDescription(false))
            .withMetadata("parameter_name", ex.getName())
            .withMetadata("expected_type", ex.getRequiredType().getSimpleName())
            .withMetadata("actual_value", ex.getValue());

        loggerUtility.logWarn("Method argument type mismatch", context);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Type Mismatch")
            .message("Invalid parameter type for: " + ex.getName())
            .path(request.getDescription(false))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // Custom exception classes
    public static class ValidationException extends RuntimeException {
        private final Map<String, String> errors;

        public ValidationException(String message, Map<String, String> errors) {
            super(message);
            this.errors = errors;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }

    public static class SecurityException extends RuntimeException {
        public SecurityException(String message) {
            super(message);
        }
    }

    public static class BusinessException extends RuntimeException {
        private final String errorCode;

        public BusinessException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public static class ResourceNotFoundException extends RuntimeException {
        private final String resourceType;
        private final String resourceId;

        public ResourceNotFoundException(String message, String resourceType, String resourceId) {
            super(message);
            this.resourceType = resourceType;
            this.resourceId = resourceId;
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getResourceId() {
            return resourceId;
        }
    }

    public static class RateLimitExceededException extends RuntimeException {
        private final int retryAfterSeconds;

        public RateLimitExceededException(String message, int retryAfterSeconds) {
            super(message);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public int getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    // Error response builder
    public static class ErrorResponse {
        private String errorId;
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private String errorCode;
        private Map<String, Object> details;
        private Integer retryAfter;

        private ErrorResponse() {}

        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }

        // Getters
        public String getErrorId() { return errorId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        public String getErrorCode() { return errorCode; }
        public Map<String, Object> getDetails() { return details; }
        public Integer getRetryAfter() { return retryAfter; }

        public static class ErrorResponseBuilder {
            private ErrorResponse response = new ErrorResponse();

            public ErrorResponseBuilder errorId(String errorId) {
                response.errorId = errorId;
                return this;
            }

            public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
                response.timestamp = timestamp;
                return this;
            }

            public ErrorResponseBuilder status(int status) {
                response.status = status;
                return this;
            }

            public ErrorResponseBuilder error(String error) {
                response.error = error;
                return this;
            }

            public ErrorResponseBuilder message(String message) {
                response.message = message;
                return this;
            }

            public ErrorResponseBuilder path(String path) {
                response.path = path;
                return this;
            }

            public ErrorResponseBuilder errorCode(String errorCode) {
                response.errorCode = errorCode;
                return this;
            }

            public ErrorResponseBuilder details(Map<String, Object> details) {
                response.details = details;
                return this;
            }

            public ErrorResponseBuilder retryAfter(Integer retryAfter) {
                response.retryAfter = retryAfter;
                return this;
            }

            public ErrorResponse build() {
                return response;
            }
        }
    }
}
