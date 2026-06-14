package com.smartmail.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.ai.entity.AiAnalysisTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiAnalysisTaskMapper extends BaseMapper<AiAnalysisTask> {
    @Select("""
            SELECT * FROM ai_analysis_task
            WHERE item_id = #{itemId}
              AND user_id = #{userId}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    AiAnalysisTask findLatestByItemAndUser(Long itemId, Long userId);
}
