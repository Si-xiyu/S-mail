package com.smartmail.user.service;

import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import com.smartmail.user.dto.UserProfileResponse;
import com.smartmail.user.entity.SysUser;
import com.smartmail.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final SysUserMapper userMapper;

    public UserService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserProfileResponse me() {
        Long userId = UserContext.requireUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getUsername(), user.getStatus());
    }
}
