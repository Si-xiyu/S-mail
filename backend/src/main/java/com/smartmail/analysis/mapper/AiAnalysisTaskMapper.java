package com.smartmail.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.analysis.entity.AiAnalysisTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiAnalysisTaskMapper extends BaseMapper<AiAnalysisTask> {

    @Select("""
            SELECT * FROM ai_analysis_task
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT #{limit}
            """)
    List<AiAnalysisTask> listPending(int limit);

    @Select("""
            SELECT * FROM ai_analysis_task
            WHERE mail_id = #{mailId}
              AND user_id = #{userId}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    AiAnalysisTask findLatestByMailAndUser(Long mailId, Long userId);

    @Select("""
            SELECT * FROM ai_analysis_task
            WHERE item_id = #{itemId}
              AND user_id = #{userId}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    AiAnalysisTask findLatestByItemAndUser(Long itemId, Long userId);
}
