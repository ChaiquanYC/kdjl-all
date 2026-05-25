package com.kdjl.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis cache abstraction replacing Memcached (kernel/memory.v1.php).
 * Key patterns mirror the original PHP Memcached usage:
 *   db:<table>:<id>         — row cache
 *   player:<uid>             — player session data
 *   chat:rooms               — chat room list
 *   session:<sid>            — user session (replaces memSession.v1.php)
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redis;
    private final ObjectMapper mapper;

    public CacheService(RedisTemplate<String, Object> redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    // ---- Generic operations ----

    public <T> Optional<T> get(String key, Class<T> type) {
        Object raw = redis.opsForValue().get(key);
        if (raw == null) return Optional.empty();
        try {
            if (raw instanceof String s) {
                return Optional.ofNullable(mapper.readValue(s, type));
            }
            return Optional.ofNullable(mapper.convertValue(raw, type));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cache key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL);
    }

    public void delete(String key) {
        redis.delete(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    public void expire(String key, Duration ttl) {
        redis.expire(key, ttl);
    }

    // ---- DB row cache (db:<table>:<id>) ----

    public <T> Optional<T> getRow(String table, Object id, Class<T> type) {
        return get("db:" + table + ":" + id, type);
    }

    public void setRow(String table, Object id, Object value) {
        set("db:" + table + ":" + id, value, Duration.ofMinutes(10));
    }

    public void invalidateRow(String table, Object id) {
        delete("db:" + table + ":" + id);
    }

    // ---- Player online cache (player:<uid>) ----

    public void setPlayerOnline(Integer uid) {
        set("player:" + uid, "1", Duration.ofMinutes(5));
    }

    public boolean isPlayerOnline(Integer uid) {
        return exists("player:" + uid);
    }

    // ---- Session management (replaces kernel/memSession.v1.php) ----

    public void saveSession(String sessionId, Object data, Duration ttl) {
        set("session:" + sessionId, data, ttl);
    }

    public <T> Optional<T> getSession(String sessionId, Class<T> type) {
        return get("session:" + sessionId, type);
    }

    public void removeSession(String sessionId) {
        delete("session:" + sessionId);
    }

    // ---- Chat ----

    public void addChatMessage(String room, String messageJson) {
        redis.opsForList().leftPush("chat:" + room, messageJson);
        redis.opsForList().trim("chat:" + room, 0, 199); // keep last 200
    }

    // ---- Rate limiting ----

    public boolean tryAcquire(String action, String key, int maxRequests, Duration window) {
        String rk = "ratelimit:" + action + ":" + key;
        Long count = redis.opsForValue().increment(rk);
        if (count != null && count == 1) {
            redis.expire(rk, window);
        }
        return count != null && count <= maxRequests;
    }

    // ---- Distributed lock ----

    public boolean tryLock(String lockKey, Duration timeout) {
        return Boolean.TRUE.equals(
            redis.opsForValue().setIfAbsent("lock:" + lockKey, "1", timeout)
        );
    }

    public void releaseLock(String lockKey) {
        delete("lock:" + lockKey);
    }
}
