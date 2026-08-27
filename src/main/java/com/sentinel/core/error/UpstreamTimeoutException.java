package com.sentinel.core.error;

public class UpstreamTimeoutException extends UpstreamServiceException {

    public UpstreamTimeoutException(String service, Throwable cause) {
        super(service, cause);
    }
}
