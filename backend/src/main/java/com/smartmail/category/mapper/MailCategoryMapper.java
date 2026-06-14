package com.smartmail.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.category.entity.MailCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MailCategoryMapper extends BaseMapper<MailCategory> {

    @Select("""
            SELECT * FROM mail_category
            WHERE user_id = #{userId}
            ORDER BY sort_order, id
            """)
    List<MailCategory> listByUser(Long userId);
}
