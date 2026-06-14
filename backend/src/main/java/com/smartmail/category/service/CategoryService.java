package com.smartmail.category.service;

import com.smartmail.category.dto.CategoryRequest;
import com.smartmail.category.dto.CategoryResponse;
import com.smartmail.category.dto.CategoryUpdateRequest;
import com.smartmail.category.entity.MailCategory;
import com.smartmail.category.entity.MailCategoryAssignment;
import com.smartmail.category.mapper.MailCategoryAssignmentMapper;
import com.smartmail.category.mapper.MailCategoryMapper;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {

    public static final String DEFAULT_OTHER = "Other";
    public static final String DEFAULT_JUNK = "Junk Mail";

    private final MailCategoryMapper categoryMapper;
    private final MailCategoryAssignmentMapper assignmentMapper;

    public CategoryService(
            MailCategoryMapper categoryMapper,
            MailCategoryAssignmentMapper assignmentMapper
    ) {
        this.categoryMapper = categoryMapper;
        this.assignmentMapper = assignmentMapper;
    }

    public List<CategoryResponse> list() {
        Long userId = UserContext.requireUserId();
        List<MailCategory> categories = categoryMapper.listByUser(userId);
        List<CategoryResponse> result = new ArrayList<>();
        for (MailCategory c : categories) {
            result.add(new CategoryResponse(c.getId(), c.getName(), c.getColor(), c.getSortOrder()));
        }
        return result;
    }

    public CategoryResponse create(CategoryRequest request) {
        Long userId = UserContext.requireUserId();
        ensureUniqueName(userId, request.name(), null);
        MailCategory entity = new MailCategory();
        entity.setUserId(userId);
        entity.setName(request.name().trim());
        entity.setColor(request.color() != null ? request.color() : "#64748b");
        entity.setSortOrder(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(entity);
        return new CategoryResponse(entity.getId(), entity.getName(), entity.getColor(), entity.getSortOrder());
    }

    public CategoryResponse update(Long categoryId, CategoryUpdateRequest request) {
        Long userId = UserContext.requireUserId();
        MailCategory category = requireOwned(categoryId, userId);
        checkNotSystem(category, "不能修改系统默认分类");
        if (request.name() != null && !request.name().isBlank()) {
            ensureUniqueName(userId, request.name().trim(), categoryId);
            category.setName(request.name().trim());
        }
        if (request.color() != null) {
            category.setColor(request.color());
        }
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
        return new CategoryResponse(category.getId(), category.getName(), category.getColor(), category.getSortOrder());
    }

    @Transactional
    public void delete(Long categoryId) {
        Long userId = UserContext.requireUserId();
        MailCategory category = requireOwned(categoryId, userId);
        checkNotSystem(category, "不能删除系统默认分类");

        MailCategory other = findDefaultCategory(userId, DEFAULT_OTHER);
        List<MailCategoryAssignment> assignments = assignmentMapper.listByCategory(categoryId, userId);
        for (MailCategoryAssignment a : assignments) {
            a.setCategoryId(other.getId());
            assignmentMapper.updateById(a);
        }
        categoryMapper.deleteById(categoryId);
    }

    @Transactional
    public void ensureDefaultCategories(Long userId) {
        if (findDefaultCategory(userId, DEFAULT_OTHER) == null) {
            MailCategory other = new MailCategory();
            other.setUserId(userId);
            other.setName(DEFAULT_OTHER);
            other.setColor("#64748b");
            other.setSortOrder(0);
            other.setCreatedAt(LocalDateTime.now());
            other.setUpdatedAt(LocalDateTime.now());
            categoryMapper.insert(other);
        }
        if (findDefaultCategory(userId, DEFAULT_JUNK) == null) {
            MailCategory junk = new MailCategory();
            junk.setUserId(userId);
            junk.setName(DEFAULT_JUNK);
            junk.setColor("#ef4444");
            junk.setSortOrder(1);
            junk.setCreatedAt(LocalDateTime.now());
            junk.setUpdatedAt(LocalDateTime.now());
            categoryMapper.insert(junk);
        }
    }

    @Transactional
    public void assignCategory(Long mailId, Long categoryId, String source) {
        Long userId = UserContext.requireUserId();
        assignCategoryForUser(mailId, userId, categoryId, source);
    }

    @Transactional
    public void assignCategoryForUser(Long mailId, Long userId, Long categoryId, String source) {
        requireOwned(categoryId, userId);

        MailCategoryAssignment existing = assignmentMapper.findByMailAndUser(mailId, userId);
        if (existing != null) {
            existing.setCategoryId(categoryId);
            existing.setAssignmentSource(source);
            assignmentMapper.updateById(existing);
        } else {
            MailCategoryAssignment assignment = new MailCategoryAssignment();
            assignment.setUserId(userId);
            assignment.setCategoryId(categoryId);
            assignment.setMailId(mailId);
            assignment.setAssignmentSource(source);
            assignment.setCreatedAt(LocalDateTime.now());
            assignmentMapper.insert(assignment);
        }
    }

    public MailCategory findDefaultCategory(Long userId, String name) {
        List<MailCategory> categories = categoryMapper.listByUser(userId);
        return categories.stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElse(null);
    }

    public MailCategory getCategoryForMail(Long mailId, Long userId) {
        MailCategoryAssignment assignment = assignmentMapper.findByMailAndUser(mailId, userId);
        if (assignment != null) {
            return categoryMapper.selectById(assignment.getCategoryId());
        }
        return findDefaultCategory(userId, DEFAULT_OTHER);
    }

    private MailCategory requireOwned(Long categoryId, Long userId) {
        MailCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !category.getUserId().equals(userId)) {
            throw new BusinessException(404, "分类不存在");
        }
        return category;
    }

    private void checkNotSystem(MailCategory category, String message) {
        if (DEFAULT_OTHER.equals(category.getName()) || DEFAULT_JUNK.equals(category.getName())) {
            throw new BusinessException(400, message);
        }
    }

    private void ensureUniqueName(Long userId, String name, Long excludeId) {
        List<MailCategory> categories = categoryMapper.listByUser(userId);
        for (MailCategory c : categories) {
            if (name.equals(c.getName()) && !c.getId().equals(excludeId)) {
                throw new BusinessException(400, "分类名称已存在: " + name);
            }
        }
    }
}
