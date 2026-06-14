package com.smartmail.user.service;

import com.smartmail.category.service.CategoryService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.UserContext;
import com.smartmail.user.dto.UserProfileResponse;
import com.smartmail.user.entity.SysUser;
import com.smartmail.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final SysUserMapper userMapper;
    private final UserSettingService settingService;
    private final CategoryService categoryService;

    public UserService(SysUserMapper userMapper, UserSettingService settingService, CategoryService categoryService) {
        this.userMapper = userMapper;
        this.settingService = settingService;
        this.categoryService = categoryService;
    }

    public UserProfileResponse me() {
        Long userId = UserContext.requireUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        categoryService.ensureDefaults(userId);
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getStatus(),
                settingService.toResponse(settingService.ensure(userId))
        );
    }
}
