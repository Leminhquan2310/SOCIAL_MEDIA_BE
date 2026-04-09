package com.social_media_be.dto.post;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostReportRequest {
    private String reason;
}
