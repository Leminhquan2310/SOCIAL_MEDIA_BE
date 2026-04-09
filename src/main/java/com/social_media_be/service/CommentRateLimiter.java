package com.social_media_be.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding window rate limiter for comment spam detection.
 * Uses ConcurrentHashMap for thread-safety without heavy synchronization.
 */
@Slf4j
@Service
public class CommentRateLimiter {

    // Map: userId → deque of timestamps (ms) within the sliding window
    private final ConcurrentHashMap<Long, Deque<Long>> userCommentTimestamps = new ConcurrentHashMap<>();

    @Value("${spam.comment.rate-limit.max-per-window:5}")
    private int maxPerWindow;

    @Value("${spam.comment.rate-limit.window-seconds:30}")
    private int windowSeconds;

    /**
     * Checks if the user has exceeded the comment rate limit.
     * Returns true if allowed, false if rate-limited.
     */
    public boolean isAllowed(Long userId) {
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        long cutoff = now - windowMs;

        // Get or create the deque for this user
        Deque<Long> timestamps = userCommentTimestamps.computeIfAbsent(userId, id -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Remove timestamps outside the sliding window
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxPerWindow) {
                log.warn("Rate limit exceeded for userId={} ({} comments in {}s)", userId, timestamps.size(), windowSeconds);
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }
}
