package com.social_media_be.service;

import com.social_media_be.dto.admin.VisitStatDto;
import com.social_media_be.dto.admin.NewUserStatDto;
import com.social_media_be.dto.admin.AdminReportStatsDto;

import java.util.List;

public interface AdminStatsService {
    List<VisitStatDto> getVisitStats(String range);
    List<NewUserStatDto> getNewUserStats(String range);
    AdminReportStatsDto getReportStats();
}
