package com.social_media_be.dto.user;

public interface UserSearchProjection {
    Long getId();
    String getUsername();
    String getFullName();
    String getAvatarUrl();
    Integer getExactMatch();
    Integer getMutualCount();
}
