package com.openclassrooms.userservice.domain;

import java.util.List;

public record PageResponse<T>(List<T> content, int currentPage, int totalPages, long totalElements, int size) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, totalPages, totalElements, size);
    }
}