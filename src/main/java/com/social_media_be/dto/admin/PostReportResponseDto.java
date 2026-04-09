package com.social_media_be.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PostReportResponseDto {
    private Long id;
    private String reporterUsername;
    private String reporterFullName;
    private String reason;
    private LocalDateTime createdAt;
}
