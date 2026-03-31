package com.social_media_be.controller;

import com.social_media_be.dto.auth.UserProfileResponse;
import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.dto.user.UpdateProfileRequest;
import com.social_media_be.dto.user.UserSummary;
import com.social_media_be.entity.User;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.service.FriendService;
import com.social_media_be.service.UserService;
import com.social_media_be.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FriendService friendService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        User user = userService.findByUserId(id);
        UserProfileResponse response;
        if (userPrincipal != null && userPrincipal.getId().equals(id)) {
            // Chủ sở hữu - thấy toàn bộ
            response = UserProfileResponse.fromEntity(user);
        } else {
            // Người khác hoặc Khách - chỉ thấy thông tin công khai
            response = UserProfileResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .avatarUrl(user.getAvatarUrl())
                    .dateOfBirth(user.getDateOfBirth())
                    .gender(user.getGender())
                    .hobby(user.getHobby())
                    .status(user.getStatus())
                    .displayFriendsStatus(user.getDisplayFriendsStatus())
                    .createdAt(user.getCreatedAt())
                    .mutualFriends(userPrincipal != null ? friendService.getMutualFriendsCount(userPrincipal.getId(), id) : null)
                    .build();
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String username) {
        User user = userService.findByUsername(username);
        UserProfileResponse response;
        if (userPrincipal != null && userPrincipal.getId().equals(user.getId())) {
            // Chủ sở hữu - thấy toàn bộ
            response = UserProfileResponse.fromEntity(user);
        } else {
            // Người khác hoặc Khách - chỉ thấy thông tin công khai
            response = UserProfileResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .avatarUrl(user.getAvatarUrl())
                    .dateOfBirth(user.getDateOfBirth())
                    .gender(user.getGender())
                    .hobby(user.getHobby())
                    .status(user.getStatus())
                    .displayFriendsStatus(user.getDisplayFriendsStatus())
                    .createdAt(user.getCreatedAt())
                    .mutualFriends(userPrincipal != null ? friendService.getMutualFriendsCount(userPrincipal.getId(), user.getId()) : null)
                    .build();
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
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

//    @GetMapping("/seach")
//    public ResponseEntity<?> searchUser(
//            @RequestParam String q,
//            @RequestParam(required = false) Long lastPostId,
//            @RequestParam(defaultValue = "10") int limit,
//            @AuthenticationPrincipal UserPrincipal userPrincipal
//    ){
//        List<UserSummary> response = userService.searchUsers(q, userPrincipal.getId(), lastPostId, limit);
//        return ResponseEntity.ok(response);
//    }
}
