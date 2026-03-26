package com.auruspay.exceptionhandler;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

public class ErrorResponse {

    public static final String HttpStatus = null;
	private LocalDateTime timestamp;
    private HttpStatus status;
    private String message;

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus  notFound) {
        this.status = notFound;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}