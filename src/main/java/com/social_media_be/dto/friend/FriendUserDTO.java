package com.social_media_be.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendUserDTO {
    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private long mutualFriendsCount;
}
