package com.smartmail.category.dto;

public record CategoryResponse(
        Long id,
        String name,
        String color,
        Boolean system
) {
}
