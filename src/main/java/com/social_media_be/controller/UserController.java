package com.social_media_be.controller;

import com.social_media_be.dto.auth.UserProfileResponse;
import com.social_media_be.dto.user.UpdateProfileRequest;
import com.social_media_be.entity.User;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.service.UserService;
import com.social_media_be.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        User user = userService.findByUserId(id);
        return ResponseEntity.ok(ApiResponse.success(UserProfileResponse.fromEntity(user)));
    }

    @PatchMapping("/profile/update")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        User updatedUser = userService.updateProfile(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(UserProfileResponse.fromEntity(updatedUser)));
    }

    @PatchMapping("/profile/avatar")
    public ResponseEntity<?> changeAvatar(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        String newAvatarUrl = userService.updateAvatar(userPrincipal.getId(), file);
        return ResponseEntity.ok(ApiResponse.success(newAvatarUrl));
    }
}
