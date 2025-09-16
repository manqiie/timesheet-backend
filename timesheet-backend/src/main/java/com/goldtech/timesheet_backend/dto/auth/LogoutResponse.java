// LogoutResponse.java
package com.goldtech.timesheet_backend.dto.auth;

public class LogoutResponse {

    private boolean success;
    private String message;

    // Constructors
    public LogoutResponse() {}

    public LogoutResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Static factory methods
    public static LogoutResponse success() {
        return new LogoutResponse(true, "Logout successful");
    }

    public static LogoutResponse failure(String message) {
        return new LogoutResponse(false, message);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}