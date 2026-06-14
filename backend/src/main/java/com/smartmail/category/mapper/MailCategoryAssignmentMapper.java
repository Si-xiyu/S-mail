package com.smartmail.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.category.entity.MailCategoryAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MailCategoryAssignmentMapper extends BaseMapper<MailCategoryAssignment> {

    @Select("""
            SELECT a.* FROM mail_category_assignment a
            JOIN mail_category c ON a.category_id = c.id
            WHERE a.mail_id = #{mailId}
              AND a.user_id = #{userId}
              AND c.user_id = #{userId}
            LIMIT 1
            """)
    MailCategoryAssignment findByMailAndUser(Long mailId, Long userId);

    @Select("""
            SELECT a.* FROM mail_category_assignment a
            JOIN mail_category c ON a.category_id = c.id
            WHERE c.id = #{categoryId}
              AND a.user_id = #{userId}
              AND c.user_id = #{userId}
            """)
    List<MailCategoryAssignment> listByCategory(Long categoryId, Long userId);

    @Select("""
            SELECT a.mail_id FROM mail_category_assignment a
            JOIN mail_category c ON a.category_id = c.id
            WHERE c.id = #{categoryId}
              AND a.user_id = #{userId}
              AND c.user_id = #{userId}
            """)
    List<Long> listMailIdsByCategory(Long categoryId, Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM mail_category_assignment a
            JOIN mail_category c ON a.category_id = c.id
            WHERE c.id = #{categoryId}
              AND a.user_id = #{userId}
              AND c.user_id = #{userId}
            """)
    long countByCategory(Long categoryId, Long userId);
}
