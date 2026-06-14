package com.smartmail.category.service;

import com.smartmail.category.dto.CategoryResponse;
import com.smartmail.category.entity.MailCategory;
import com.smartmail.category.mapper.MailCategoryMapper;
import com.smartmail.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryService {
    private final MailCategoryMapper categoryMapper;

    public CategoryService(MailCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Transactional
    public void ensureDefaults(Long userId) {
        ensureCategory(userId, "Other", "#64748b");
        ensureCategory(userId, "Junk Mail", "#ef4444");
    }

    public List<CategoryResponse> listMine() {
        Long userId = UserContext.requireUserId();
        ensureDefaults(userId);
        return categoryMapper.listByUser(userId).stream().map(this::toResponse).toList();
    }

    private void ensureCategory(Long userId, String name, String color) {
        if (categoryMapper.findByUserAndName(userId, name) != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        MailCategory category = new MailCategory();
        category.setUserId(userId);
        category.setName(name);
        category.setColor(color);
        category.setSystemFlag(true);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        categoryMapper.insert(category);
    }

    private CategoryResponse toResponse(MailCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getColor(), category.getSystemFlag());
    }
}
