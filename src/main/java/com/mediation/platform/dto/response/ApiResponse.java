package com.mediation.platform.dto.response;

import java.time.LocalDateTime;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String error;

    // Constructeurs
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, String message, T data) {
        this(success, message);
        this.data = data;
    }

    // Méthodes statiques pour créer des réponses - CORRIGÉES
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Object> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static ApiResponse<Object> error(String message) {
        ApiResponse<Object> response = new ApiResponse<>(false, message);
        response.setError(message);
        return response;
    }

    public static <T> ApiResponse<T> error(String message, T errorData) {
        ApiResponse<T> response = new ApiResponse<>(false, message);
        response.setError(message);
        response.setData(errorData);
        return response;
    }

    // Getters et Setters
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}