package com.smartmail.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.ai.entity.MailAiResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MailAiResultMapper extends BaseMapper<MailAiResult> {
    @Select("""
            SELECT * FROM mail_ai_result
            WHERE mail_id = #{mailId}
              AND user_id = #{userId}
            ORDER BY created_at DESC
            """)
    List<MailAiResult> listByMailAndUser(Long mailId, Long userId);
}
