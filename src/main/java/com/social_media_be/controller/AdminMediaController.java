package com.social_media_be.controller;

import com.social_media_be.entity.enums.MediaStatus;
import com.social_media_be.projection.AdminMediaProjection;
import com.social_media_be.service.AdminMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMediaController {

    private final AdminMediaService adminMediaService;

    @GetMapping
    public ResponseEntity<Page<AdminMediaProjection>> getMediaStream(
            @RequestParam(required = false) MediaStatus status,
            @RequestParam(required = false) Double minScore,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminMediaService.getMediaStream(status, minScore, pageable));
    }

    @PatchMapping("/status")
    public ResponseEntity<Void> updateMediaStatus(
            @RequestParam String sourceType,
            @RequestParam Long id,
            @RequestParam MediaStatus status) {
        adminMediaService.updateStatus(sourceType, id, status);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/bulk-status")
    public ResponseEntity<Void> bulkUpdateStatus(
            @RequestBody Map<String, Object> request) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        MediaStatus status = MediaStatus.valueOf((String) request.get("status"));
        adminMediaService.bulkUpdateStatus(items, status);
        return ResponseEntity.ok().build();
    }
}
