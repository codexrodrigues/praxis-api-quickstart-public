package com.example.praxis.apiquickstart.core;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter simples (in-memory, janela fixa) por chave.
 * Adequado para demonstração/local. Para produção, use gateway/WAF ou Bucket4j.
 */
@Service
public class RateLimiterService {
    private static final int DEFAULT_LIMIT = 10;          // 10 requisições
    private static final long DEFAULT_WINDOW_MS = 60_000; // por minuto

    private static class Window {
        volatile long windowStart;
        AtomicInteger count = new AtomicInteger(0);
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String key) {
        return allow(key, DEFAULT_LIMIT, DEFAULT_WINDOW_MS);
    }

    public boolean allow(String key, int limit, long windowMs) {
        long now = Instant.now().toEpochMilli();
        Window w = windows.computeIfAbsent(key, k -> { Window x = new Window(); x.windowStart = now; return x; });
        synchronized (w) {
            if (now - w.windowStart >= windowMs) {
                w.windowStart = now;
                w.count.set(0);
            }
            if (w.count.incrementAndGet() > limit) {
                return false;
            }
        }
        return true;
    }
}

