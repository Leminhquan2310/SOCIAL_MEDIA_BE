package com.social_media_be.exception;

public class AccessDeniedPermissionException extends RuntimeException {
    public AccessDeniedPermissionException(String message) {
        super(message);
    }
    public AccessDeniedPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
