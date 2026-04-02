package com.social_media_be.dto.post;

import com.social_media_be.dto.user.UserSummary;
import com.social_media_be.entity.enums.Privacy;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostResponse {
    private Long id;
    private String content;
    private Privacy privacy;
    private String feeling;
    private UserSummary author;
    private List<PostImageDto> images;
    private Integer likeCount;
    private Integer commentCount;
    private boolean isLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
