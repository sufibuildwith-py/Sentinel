package com.sentinel.core.error;

public class InvalidModelResponseException extends RuntimeException {

    public InvalidModelResponseException(String message) {
        super(message);
    }

    public InvalidModelResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
