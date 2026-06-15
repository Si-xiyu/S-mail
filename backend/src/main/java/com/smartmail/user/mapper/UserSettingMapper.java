package com.smartmail.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.user.entity.UserSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserSettingMapper extends BaseMapper<UserSetting> {
    @Select("""
            SELECT * FROM user_setting
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    UserSetting findByUserId(Long userId);
}
