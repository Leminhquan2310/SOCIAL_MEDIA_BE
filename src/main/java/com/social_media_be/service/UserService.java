package com.social_media_be.service;

import com.social_media_be.dto.user.UpdateProfileRequest;
import com.social_media_be.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User findByUserId(Long id);

    User findByUsername(String username);

    User updateProfile(Long userId, UpdateProfileRequest request);

    String updateAvatar(Long userId, MultipartFile file);
}
