package com.social_media_be.dto.post;

import com.social_media_be.entity.enums.MediaType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostImageDto {
    private Long id;
    private String mediaUrl;
    private MediaType mediaType;
    private Integer orderIndex;
    private com.social_media_be.entity.enums.MediaStatus status;
}
