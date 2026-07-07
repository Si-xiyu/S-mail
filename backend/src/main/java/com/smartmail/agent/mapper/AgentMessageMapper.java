package com.smartmail.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.agent.entity.AgentMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {
    @Select("SELECT * FROM agent_message WHERE session_id = #{sessionId} AND user_id = #{userId} ORDER BY id ASC")
    List<AgentMessage> listBySessionAndUser(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}
