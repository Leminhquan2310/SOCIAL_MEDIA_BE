package com.social_media_be.service;

import com.social_media_be.entity.enums.TargetType;

public interface LikeService {
    void toggleLike(Long targetId, TargetType targetType, Long userId);
    boolean isLiked(Long targetId, TargetType targetType, Long userId);
    long getLikeCount(Long targetId, TargetType targetType);
}
