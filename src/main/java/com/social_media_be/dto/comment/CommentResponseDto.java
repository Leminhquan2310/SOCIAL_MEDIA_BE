package com.social_media_be.dto.comment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponseDto {
    private Long id;
    private Long postId;
    
    private Long authorId;
    private String authorName;
    private String authorUsername;
    private String authorAvatar;
    
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Long parentCommentId;
    private String parentCommentName;
    private String parentCommentUsername;
    
    // Thống kê bình luận
    private long likeCount;
    private boolean isLiked;
    private long replyCount;
    private boolean edited;
}
