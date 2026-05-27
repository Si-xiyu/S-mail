package com.smartmail.auth.service;

import com.smartmail.auth.dto.AuthResponse;
import com.smartmail.auth.dto.LoginRequest;
import com.smartmail.auth.dto.RegisterRequest;
import com.smartmail.common.exception.BusinessException;
import com.smartmail.common.security.TokenService;
import com.smartmail.user.entity.SysUser;
import com.smartmail.user.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userMapper.findByEmail(request.email()) != null) {
            throw new BusinessException("邮箱已注册");
        }
        SysUser user = new SysUser();
        user.setEmail(request.email().trim().toLowerCase());
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        SysUser user = userMapper.findByEmail(request.email().trim().toLowerCase());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "邮箱或密码错误");
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(SysUser user) {
        return new AuthResponse(tokenService.createToken(user.getId()), user.getId(), user.getEmail(), user.getUsername());
    }
}
