package com.usang.stockmarket.api.dto;

public record ApiResponse<T> (
    boolean success,
    String message,
    T data
){
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
