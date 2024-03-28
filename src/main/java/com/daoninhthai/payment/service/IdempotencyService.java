package com.daoninhthai.payment.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private static final long TTL_HOURS = 24;

    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    public Optional<Object> checkAndGet(String key) {
        CachedResponse cached = cache.get(key);
        if (cached == null) {
            return Optional.empty();
        }

        // Check if expired (24 hours)
        if (Instant.now().isAfter(cached.expiresAt)) {
            cache.remove(key);
            return Optional.empty();
        }

        return Optional.of(cached.response);
    }

    public void store(String key, Object response) {
        Instant expiresAt = Instant.now().plusSeconds(TTL_HOURS * 3600);
        cache.put(key, new CachedResponse(response, expiresAt));
    }

    public boolean exists(String key) {
        return checkAndGet(key).isPresent();
    }

    private record CachedResponse(Object response, Instant expiresAt) {
    }
}
