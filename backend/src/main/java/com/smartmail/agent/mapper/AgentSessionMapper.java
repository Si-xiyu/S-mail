package com.smartmail.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.agent.entity.AgentSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {
    @Select("SELECT * FROM agent_session WHERE session_id = #{sessionId} AND user_id = #{userId}")
    AgentSession findBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}
