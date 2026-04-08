package com.social_media_be.service;

import com.social_media_be.dto.admin.VisitStatDto;

import java.util.List;

public interface AdminStatsService {
    List<VisitStatDto> getVisitStats(String range);
}
