package com.kdjl.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    int code,
    String message,
    T data,
    Long total,
    Integer page,
    Integer limit
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "ok", data, null, null, null);
    }

    public static <T> ApiResponse<T> success(T data, long total, int page, int limit) {
        return new ApiResponse<>(0, "ok", data, total, page, limit);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(-1, message, null, null, null, null);
    }
}
