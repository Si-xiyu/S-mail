package com.smartmail.common.security;

import com.smartmail.user.entity.SysUser;
import com.smartmail.user.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final SysUserMapper userMapper;

    public AuthTokenFilter(TokenService tokenService, SysUserMapper userMapper) {
        this.tokenService = tokenService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                Long userId = tokenService.verify(header.substring(7));
                SysUser user = userMapper.selectById(userId);
                if (user != null) {
                    UserContext.set(new CurrentUser(user.getId(), user.getEmail(), user.getUsername()));
                }
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
