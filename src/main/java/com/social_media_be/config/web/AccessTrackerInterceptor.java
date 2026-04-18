package com.social_media_be.config.web;

import com.social_media_be.service.AccessCounterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts all incoming API requests to count total web traffic.
 * Excludes admin-only endpoints to avoid counting internal dashboard calls.
 */
@Component
@RequiredArgsConstructor
public class AccessTrackerInterceptor implements HandlerInterceptor {

    private final AccessCounterService accessCounterService;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        String path = request.getRequestURI();

        // Exclude admin stat endpoints to avoid self-inflating the counters
        boolean isAdminStatsEndpoint = path.startsWith("/api/admin/stats");

        if (!isAdminStatsEndpoint) {
            accessCounterService.increment();
        }

        return true;
    }
}
