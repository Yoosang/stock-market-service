package com.usang.stockmarket.application.access;

import com.usang.stockmarket.infra.telegram.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class VisitTrackingService {

    private static final String KEY_PREFIX = "access:mainpage:";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final long DAILY_ALERT_THRESHOLD = 5;
    private static final Duration KEY_TTL = Duration.ofHours(26);

    private final StringRedisTemplate redisTemplate;
    private final TelegramNotifier telegramNotifier;

    public void trackMainPageVisit(String ip) {
        String key = KEY_PREFIX + ip + ":" + LocalDate.now(KST);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, KEY_TTL);
        }
        if (count != null && count >= DAILY_ALERT_THRESHOLD) {
            telegramNotifier.sendAsync("[접속 알림] IP %s 에서 오늘 메인 페이지에 %d회 접속했습니다.".formatted(ip, count));
            redisTemplate.delete(key);
        }
    }
}
