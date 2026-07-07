package com.smartmail.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.agent.entity.AgentPendingAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentPendingActionMapper extends BaseMapper<AgentPendingAction> {
    @Select("SELECT * FROM agent_pending_action WHERE session_id = #{sessionId} AND user_id = #{userId} ORDER BY id ASC")
    List<AgentPendingAction> listBySessionAndUser(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    @Select("SELECT * FROM agent_pending_action WHERE action_id = #{actionId} AND user_id = #{userId}")
    AgentPendingAction findByActionIdAndUserId(@Param("actionId") String actionId, @Param("userId") Long userId);
}
