package com.social_media_be.dto.comment;

import com.social_media_be.entity.enums.MediaType;
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
    private String mediaUrl;
    private MediaType mediaType;
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
    private com.social_media_be.entity.enums.MediaStatus mediaStatus;
}
