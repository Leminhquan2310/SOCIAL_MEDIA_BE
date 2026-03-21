package com.social_media_be.service.implement;

import com.social_media_be.dto.user.UpdateProfileRequest;
import com.social_media_be.entity.User;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.CloudinaryService;
import com.social_media_be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    @Value("${app.avatar.default-url}")
    private String avatarDefault;

    @Override
    public User findByUserId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findByUserId(userId);
        
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getHobby() != null) user.setHobby(request.getHobby());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public String updateAvatar(Long userId, MultipartFile file) {
        User user = findByUserId(userId);
        String oldAvatarUrl = user.getAvatarUrl();
        
        try {
            // Delete old avatar if it's not the default one
            if (oldAvatarUrl != null && !oldAvatarUrl.equals(avatarDefault)) {
                String publicId = cloudinaryService.extractPublicId(oldAvatarUrl);
                if (publicId != null) {
                    cloudinaryService.delete(publicId);
                }
            }

            Map uploadResult = cloudinaryService.upload(file, "social-media/avatars");
            String avatarUrl = (String) uploadResult.get("url");
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            return avatarUrl;
        } catch (IOException e) {
            throw new RuntimeException("Upload avatar failed: " + e.getMessage());
        }
    }
}
