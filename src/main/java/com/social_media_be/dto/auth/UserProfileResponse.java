package com.social_media_be.dto.auth;

import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.AuthProvider;
import com.social_media_be.entity.enums.DisplayFriendsStatus;
import com.social_media_be.entity.enums.Gender;
import com.social_media_be.entity.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String hobby;
    private String address;
    private UserStatus status;
    private DisplayFriendsStatus displayFriendsStatus;
    private AuthProvider authProvider;
    private LocalDateTime createdAt;
    private Set<String> roles;
    private Integer mutualFriends;

    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .hobby(user.getHobby())
                .address(user.getAddress())
                .status(user.getStatus())
                .displayFriendsStatus(user.getDisplayFriendsStatus())
                .authProvider(user.getAuthProvider())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()))
                .build();
    }
}
