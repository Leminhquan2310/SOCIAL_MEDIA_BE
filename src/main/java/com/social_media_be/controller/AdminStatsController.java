package com.social_media_be.controller;

import com.social_media_be.dto.admin.AdminReportStatsDto;
import com.social_media_be.dto.admin.VisitStatDto;
import com.social_media_be.service.AdminStatsService;
import com.social_media_be.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    /**
     * Get visit statistics for a given time range.
     *
     * @param range "week" (default, 7 days), "month" (30 days), or "year" (365 days)
     */
    @GetMapping("/visits")
    public ResponseEntity<?> getVisitStats(
            @RequestParam(defaultValue = "week") String range) {

        List<VisitStatDto> stats = adminStatsService.getVisitStats(range);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/new-users")
    public ResponseEntity<?> getNewUserStats(
            @RequestParam(defaultValue = "week") String range) {
        List<com.social_media_be.dto.admin.NewUserStatDto> stats = adminStatsService.getNewUserStats(range);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getReportStats() {
        AdminReportStatsDto stats = adminStatsService.getReportStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
