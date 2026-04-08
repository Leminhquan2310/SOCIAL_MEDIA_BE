package com.social_media_be.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory counter for web traffic visits.
 * Uses AtomicLong for thread-safe counting without database writes on every request.
 * The StatFlushScheduler will periodically flush this count to the database.
 */
@Component
public class AccessCounterService {

    private final AtomicLong counter = new AtomicLong(0);

    public void increment() {
        counter.incrementAndGet();
    }

    /**
     * Returns the current count and resets to 0 atomically.
     * Used by the flush scheduler to get the count and clear it.
     */
    public long getAndReset() {
        return counter.getAndSet(0);
    }
}
