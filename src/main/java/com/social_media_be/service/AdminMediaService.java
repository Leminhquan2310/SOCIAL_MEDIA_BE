package com.social_media_be.service;

import com.social_media_be.entity.enums.MediaStatus;
import com.social_media_be.projection.AdminMediaProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AdminMediaService {
    Page<AdminMediaProjection> getMediaStream(MediaStatus status, Double minScore, Pageable pageable);
    void updateStatus(String sourceType, Long id, MediaStatus status);
    void bulkUpdateStatus(List<Map<String, Object>> items, MediaStatus status);
}
