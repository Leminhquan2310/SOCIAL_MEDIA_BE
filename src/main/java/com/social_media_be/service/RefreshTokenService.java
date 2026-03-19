package com.social_media_be.service;

import com.social_media_be.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);
    RefreshToken findByToken(String token);
}
