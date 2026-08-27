package com.sentinel.core.error;

public class UpstreamServiceException extends RuntimeException {

    private final String service;
    private final Integer upstreamStatus;

    public UpstreamServiceException(String service, int upstreamStatus) {
        super(service + " request failed");
        this.service = service;
        this.upstreamStatus = upstreamStatus;
    }

    public UpstreamServiceException(String service, Throwable cause) {
        super(service + " request failed", cause);
        this.service = service;
        this.upstreamStatus = null;
    }

    public String getService() {
        return service;
    }

    public Integer getUpstreamStatus() {
        return upstreamStatus;
    }
}
