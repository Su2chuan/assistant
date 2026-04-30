package com.assistant.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 滑动窗口限流 Lua 脚本
     * KEYS[1] = 限流 key
     * ARGV[1] = 窗口大小（秒）
     * ARGV[2] = 最大请求数
     * ARGV[3] = 当前时间戳（毫秒）
     */
    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local window = tonumber(ARGV[1])
            local maxCount = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local windowStart = now - window * 1000

            redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
            local count = redis.call('ZCARD', key)

            if count < maxCount then
                redis.call('ZADD', key, now, now .. ':' .. math.random(1, 1000000))
                redis.call('EXPIRE', key, window)
                return 1
            end
            return 0
            """;

    private final DefaultRedisScript<Long> rateLimitScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    /**
     * 尝试请求，返回是否允许
     *
     * @param key      限流 key（如 "rate:chat:127.0.0.1"）
     * @param maxCount 窗口内最大请求数
     * @param window   窗口大小（秒）
     * @return true=允许，false=限流
     */
    public boolean tryAcquire(String key, int maxCount, int window) {
        try {
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(window),
                    String.valueOf(maxCount),
                    String.valueOf(System.currentTimeMillis())
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("限流检查异常，放行请求: {}", e.getMessage());
            return true;
        }
    }
}
