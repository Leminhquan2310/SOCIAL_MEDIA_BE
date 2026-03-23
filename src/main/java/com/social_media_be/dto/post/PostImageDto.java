package com.social_media_be.dto.post;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostImageDto {
    private Long id;
    private String imageUrl;
    private Integer orderIndex;
}
