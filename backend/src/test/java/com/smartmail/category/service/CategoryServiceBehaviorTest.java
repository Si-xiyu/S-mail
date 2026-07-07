package com.smartmail.category.service;

import com.smartmail.category.entity.MailCategory;
import com.smartmail.category.entity.MailCategoryAssignment;
import com.smartmail.category.mapper.MailCategoryAssignmentMapper;
import com.smartmail.category.mapper.MailCategoryMapper;
import com.smartmail.common.security.CurrentUser;
import com.smartmail.common.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceBehaviorTest {
    private MailCategoryMapper categoryMapper;
    private MailCategoryAssignmentMapper assignmentMapper;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        categoryMapper = mock(MailCategoryMapper.class);
        assignmentMapper = mock(MailCategoryAssignmentMapper.class);
        service = new CategoryService(categoryMapper, assignmentMapper);
        UserContext.set(new CurrentUser(1L, "user@smail.com", "User"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void assigningSameLabelTwiceIsIdempotentAndDifferentLabelsAreAllowed() {
        when(categoryMapper.selectById(10L)).thenReturn(category(10L, "Project"));
        when(categoryMapper.selectById(11L)).thenReturn(category(11L, "Course"));
        when(assignmentMapper.findByMailAndUserAndCategory(900L, 1L, 10L))
                .thenReturn(null, assignment(100L, 10L));
        when(assignmentMapper.findByMailAndUserAndCategory(900L, 1L, 11L)).thenReturn(null);

        service.assignCategory(900L, 10L, "MANUAL");
        service.assignCategory(900L, 10L, "MANUAL");
        service.assignCategory(900L, 11L, "MANUAL");

        ArgumentCaptor<MailCategoryAssignment> captor = ArgumentCaptor.forClass(MailCategoryAssignment.class);
        verify(assignmentMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(MailCategoryAssignment::getCategoryId)
                .containsExactly(10L, 11L);
    }

    @Test
    void deletingLabelDoesNotCreateDuplicateOtherAssignment() {
        MailCategory custom = category(10L, "Project");
        MailCategory other = category(20L, CategoryService.DEFAULT_OTHER);
        MailCategoryAssignment customAssignment = assignment(101L, 10L);
        when(categoryMapper.selectById(10L)).thenReturn(custom);
        when(categoryMapper.listByUser(1L)).thenReturn(List.of(other, custom));
        when(assignmentMapper.listByCategory(10L, 1L)).thenReturn(List.of(customAssignment));
        when(assignmentMapper.findByMailAndUserAndCategory(900L, 1L, 20L))
                .thenReturn(assignment(102L, 20L));

        service.delete(10L);

        verify(assignmentMapper).deleteById(101L);
        verify(assignmentMapper, never()).updateById(any(MailCategoryAssignment.class));
        verify(categoryMapper).deleteById(10L);
    }

    private MailCategory category(Long id, String name) {
        MailCategory category = new MailCategory();
        category.setId(id);
        category.setUserId(1L);
        category.setName(name);
        return category;
    }

    private MailCategoryAssignment assignment(Long id, Long categoryId) {
        MailCategoryAssignment assignment = new MailCategoryAssignment();
        assignment.setId(id);
        assignment.setUserId(1L);
        assignment.setMailId(900L);
        assignment.setCategoryId(categoryId);
        return assignment;
    }
}
