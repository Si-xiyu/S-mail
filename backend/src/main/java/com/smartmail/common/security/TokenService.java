package com.smartmail.common.security;

import com.smartmail.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {
    private final String secret;
    private final long ttlSeconds;

    public TokenService(
            @Value("${smartmail.auth.token-secret}") String secret,
            @Value("${smartmail.auth.token-ttl-seconds}") long ttlSeconds
    ) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String createToken(Long userId) {
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = userId + ":" + expiresAt;
        return encode(payload + ":" + sign(payload));
    }

    public Long verify(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 3) {
                throw new BusinessException(401, "无效 token");
            }
            String payload = parts[0] + ":" + parts[1];
            if (!sign(payload).equals(parts[2])) {
                throw new BusinessException(401, "无效 token");
            }
            if (Long.parseLong(parts[1]) < Instant.now().getEpochSecond()) {
                throw new BusinessException(401, "token 已过期");
            }
            return Long.parseLong(parts[0]);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(401, "无效 token");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("token 签名失败", ex);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
