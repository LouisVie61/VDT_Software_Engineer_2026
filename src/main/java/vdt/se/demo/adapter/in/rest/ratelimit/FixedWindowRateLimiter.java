package vdt.se.demo.adapter.in.rest.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowRateLimiter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final Clock clock;

    public FixedWindowRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean allow(String key, int maxRequests, int windowSeconds) {
        if (maxRequests <= 0 || windowSeconds <= 0) {
            return false;
        }
        long now = Instant.now(clock).getEpochSecond();
        long windowStart = now - (now % windowSeconds);
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.windowStart() != windowStart) {
                return new WindowCounter(windowStart, new AtomicInteger(0));
            }
            return existing;
        });
        return counter.count().incrementAndGet() <= maxRequests;
    }

    private record WindowCounter(long windowStart, AtomicInteger count) {
    }
}
