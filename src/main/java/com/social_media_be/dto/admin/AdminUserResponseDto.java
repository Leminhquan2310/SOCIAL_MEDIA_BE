package com.social_media_be.dto.admin;

import com.social_media_be.entity.Role;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.AuthProvider;
import com.social_media_be.entity.enums.DisplayFriendsStatus;
import com.social_media_be.entity.enums.Gender;
import com.social_media_be.entity.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class AdminUserResponseDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String address;
    private String phone;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String hobby;
    private UserStatus status;
    private DisplayFriendsStatus displayFriendsStatus;
    private AuthProvider authProvider;
    private LocalDateTime createdAt;
    private boolean enabled;
    private Set<String> roles;

    public static AdminUserResponseDto fromEntity(User user) {
        return AdminUserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .address(user.getAddress())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .hobby(user.getHobby())
                .status(user.getStatus())
                .displayFriendsStatus(user.getDisplayFriendsStatus())
                .authProvider(user.getAuthProvider())
                .createdAt(user.getCreatedAt())
                .enabled(user.isEnabled())
                .roles(user.getRoles() != null ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()) : null)
                .build();
    }
}
