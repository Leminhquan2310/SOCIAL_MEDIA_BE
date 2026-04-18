package com.social_media_be.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendStatusDTO {
    private String status;
    private Long friendshipId;
    private Long requesterId;
}
