// UserResponse.java
package com.goldtech.timesheet_backend.dto.user;

import java.util.List;

public class UserResponse {

    private boolean success;
    private String message;
    private Object data;
    private Long totalElements;

    // Constructors
    public UserResponse() {}

    public UserResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public UserResponse(boolean success, String message, Object data, Long totalElements) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.totalElements = totalElements;
    }

    // Static factory methods
    public static UserResponse success(Object data) {
        return new UserResponse(true, "Success", data);
    }

    public static UserResponse success(Object data, String message) {
        return new UserResponse(true, message, data);
    }

    public static UserResponse success(List<?> data, Long totalElements) {
        return new UserResponse(true, "Success", data, totalElements);
    }

    public static UserResponse error(String message) {
        return new UserResponse(false, message, null);
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }
}