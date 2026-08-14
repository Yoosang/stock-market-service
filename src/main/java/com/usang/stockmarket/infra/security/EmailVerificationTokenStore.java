package com.usang.stockmarket.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailVerificationTokenStore {

    private static final String KEY_PREFIX = "email-verify:";
    private static final String VERIFIED_KEY_PREFIX = "email-verified:";
    private static final Duration TTL = Duration.ofMinutes(3);
    // 인증번호 확인 후 회원가입을 마칠 때까지 여유를 주는 시간(인증번호 자체의 TTL과는 별개)
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public String issue(String email) {
        String token = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(KEY_PREFIX + email, token, TTL);
        return token;
    }

    public Optional<String> consume(String email) {
        String token = redisTemplate.opsForValue().get(KEY_PREFIX + email);
        return Optional.ofNullable(token);
    }

    public void invalidate(String email) {
        redisTemplate.delete(KEY_PREFIX + email);
    }

    public void markVerified(String email) {
        redisTemplate.opsForValue().set(VERIFIED_KEY_PREFIX + email, "true", VERIFIED_TTL);
    }

    public boolean consumeVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.delete(VERIFIED_KEY_PREFIX + email));
    }
}
