package com.social_media_be.exception;


import com.social_media_be.utils.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle UsernameNotFoundException (User not found or disabled)
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFoundException(
            UsernameNotFoundException ex,
            HttpServletRequest request) {

        log.error("UsernameNotFoundException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("UNAUTHORIZED")
                .data(buildErrorDetails(ex.getMessage(), request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle BadCredentialsException (Wrong password)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request) {

        log.error("BadCredentialsException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("UNAUTHORIZED")
                .data(buildErrorDetails("Invalid username or password", request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle DisabledException (User account disabled)
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleDisabledException(
            DisabledException ex,
            HttpServletRequest request) {

        log.error("DisabledException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.FORBIDDEN.value())
                .message("FORBIDDEN")
                .data(buildErrorDetails("User account is disabled. Please contact administrator.", request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle LockedException (User account locked)
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleLockedException(
            LockedException ex,
            HttpServletRequest request) {

        log.error("LockedException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.FORBIDDEN.value())
                .message("FORBIDDEN")
                .data(buildErrorDetails("User account is locked", request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle general AuthenticationException
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        log.error("AuthenticationException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("UNAUTHORIZED")
                .data(buildErrorDetails("Authentication failed: " + ex.getMessage(), request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle AccessDeniedException (Insufficient permissions)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.error("AccessDeniedException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.FORBIDDEN.value())
                .message("FORBIDDEN")
                .data(buildErrorDetails("Access denied. You don't have permission to access this resource.", request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ========== JWT Exceptions ==========

    /**
     * Handle ExpiredJwtException (Token expired)
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredJwtException(
            ExpiredJwtException ex,
            HttpServletRequest request) {

        log.error("ExpiredJwtException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("UNAUTHORIZED")
                .data(buildErrorDetails("JWT token has expired. Please login again.", request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle MalformedJwtException (Invalid token format)
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleMalformedJwtException(
            MalformedJwtException ex,
            HttpServletRequest request) {

        log.error("MalformedJwtException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("UNAUTHORIZED")
                .data(buildErrorDetails("Invalid JWT token format", request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle SignatureException (Invalid token signature)
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ApiResponse<Object>> handleSignatureException(
            SignatureException ex,
            HttpServletRequest request) {

        log.error("SignatureException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("UNAUTHORIZED")
                .data(buildErrorDetails("Invalid JWT signature", request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ========== Business Logic Exceptions ==========

    /**
     * Handle UnauthorizedException (Custom)
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {

        log.error("UnauthorizedException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("UNAUTHORIZED")
                .data(buildErrorDetails(ex.getMessage(), request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle BadRequestException (Custom)
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request) {

        log.error("BadRequestException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("BAD_REQUEST")
                .data(buildErrorDetails(ex.getMessage(), request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle ResourceNotFoundException (Custom)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.error("ResourceNotFoundException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.NOT_FOUND.value())
                .message("NOT_FOUND")
                .data(buildErrorDetails(ex.getMessage(), request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ========== Validation Exceptions ==========

    /**
     * Handle MethodArgumentNotValidException (Validation errors)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.error("MethodArgumentNotValidException - URI: {}", request.getRequestURI());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("errors", errors);

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("VALIDATION_FAILED")
                .data(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ========== File Upload Exceptions ==========

    /**
     * Handle MaxUploadSizeExceededException (File too large)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        log.error("MaxUploadSizeExceededException: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("BAD_REQUEST")
                .data(buildErrorDetails("File size exceeds maximum allowed size", request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ========== General Exception ==========

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception: {} - URI: {}", ex.getMessage(), request.getRequestURI(), ex);

        ApiResponse<Object> response = ApiResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("INTERNAL_SERVER_ERROR")
                .data(buildErrorDetails("An unexpected error occurred. Please try again later.", request.getRequestURI()))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ========== Helper Methods ==========

    /**
     * Build error details map
     */
    private Map<String, Object> buildErrorDetails(String message, String path) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("message", message);
        errorDetails.put("path", path);
        return errorDetails;
    }
}
