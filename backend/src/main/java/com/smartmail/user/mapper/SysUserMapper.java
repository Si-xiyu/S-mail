package com.smartmail.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmail.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("SELECT * FROM sys_user WHERE email = #{email} LIMIT 1")
    SysUser findByEmail(String email);
}
