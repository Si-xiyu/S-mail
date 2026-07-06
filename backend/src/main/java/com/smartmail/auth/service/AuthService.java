package com.smartmail.auth.service;

import com.smartmail.auth.dto.AuthResponse;
import com.smartmail.auth.dto.LoginRequest;
import com.smartmail.auth.dto.RegisterRequest;
import com.smartmail.category.service.CategoryService;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.TokenService;
import com.smartmail.common.validation.SmartMailAddress;
import com.smartmail.user.entity.SysUser;
import com.smartmail.user.mapper.SysUserMapper;
import com.smartmail.user.service.UserSettingService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserSettingService settingService;
    private final CategoryService categoryService;

    public AuthService(
            SysUserMapper userMapper,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            UserSettingService settingService,
            CategoryService categoryService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.settingService = settingService;
        this.categoryService = categoryService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = SmartMailAddress.normalize(request.email());
        if (!SmartMailAddress.isAllowed(email)) {
            throw new BusinessException(400, "邮箱必须使用 @smail.com 后缀");
        }
        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException("Email is already registered");
        }
        SysUser user = new SysUser();
        user.setEmail(email);
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        settingService.ensure(user.getId());
        categoryService.ensureDefaultCategories(user.getId());
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = SmartMailAddress.normalize(request.email());
        if (!SmartMailAddress.isAllowed(email)) {
            throw new BusinessException(401, "Email or password is incorrect");
        }
        SysUser user = userMapper.findByEmail(email);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "Email or password is incorrect");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "Account is disabled");
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(SysUser user) {
        return new AuthResponse(tokenService.createToken(user.getId()), user.getId(), user.getEmail(), user.getUsername());
    }
}
