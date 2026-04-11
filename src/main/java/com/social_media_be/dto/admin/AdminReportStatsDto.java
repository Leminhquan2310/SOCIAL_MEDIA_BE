package com.social_media_be.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminReportStatsDto {
    private long totalReportCount;
    private long totalReportedPosts;
}
