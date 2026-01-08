//package com.openclassrooms.authorizationserverservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@JsonInclude(JsonInclude.Include.NON_NULL)
//public class ApiResponse<T> {
//
//    private LocalDateTime timestamp;
//    private int status;
//    private boolean success;
//    private String message;
//    private T data;
//
//    public static <T> ApiResponse<T> success(String message, T data) {
//        return ApiResponse.<T>builder()
//                .timestamp(LocalDateTime.now())
//                .status(200)
//                .success(true)
//                .message(message)
//                .data(data)
//                .build();
//    }
//
//    public static <T> ApiResponse<T> success(String message) {
//        return success(message, null);
//    }
//
//    public static <T> ApiResponse<T> created(String message, T data) {
//        return ApiResponse.<T>builder()
//                .timestamp(LocalDateTime.now())
//                .status(201)
//                .success(true)
//                .message(message)
//                .data(data)
//                .build();
//    }
//
//    public static <T> ApiResponse<T> error(int status, String message) {
//        return ApiResponse.<T>builder()
//                .timestamp(LocalDateTime.now())
//                .status(status)
//                .success(false)
//                .message(message)
//                .build();
//    }
//}
