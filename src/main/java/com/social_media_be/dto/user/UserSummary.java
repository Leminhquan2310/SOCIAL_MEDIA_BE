package com.social_media_be.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummary {
    private Long id;
    private String fullName;
    private String avatarUrl;
}
