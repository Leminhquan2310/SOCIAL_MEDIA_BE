package com.social_media_be.dto.admin;

import com.social_media_be.entity.enums.Privacy;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminPostResponseDto {
    private Long postId;
    private Long userId;
    private String username;
    private String content;
    private Privacy status; // Mapping privacy to status in UI
    private Integer reportCount;
    private LocalDateTime createdAt;
}
