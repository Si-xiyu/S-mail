package com.smartmail.category.controller;

import com.smartmail.category.dto.CategoryRequest;
import com.smartmail.category.dto.CategoryResponse;
import com.smartmail.category.dto.CategoryUpdateRequest;
import com.smartmail.category.service.CategoryService;
import com.smartmail.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.ok(categoryService.list());
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @PatchMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> update(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ApiResponse.ok(categoryService.update(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ApiResponse.ok();
    }
}
