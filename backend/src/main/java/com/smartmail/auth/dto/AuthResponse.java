package com.smartmail.auth.dto;

public record AuthResponse(String token, Long userId, String email, String username) {
}
