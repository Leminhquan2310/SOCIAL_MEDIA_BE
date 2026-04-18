package com.social_media_be.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseDto {
    private Long id;
    private Long otherUserId;
    private String otherUserFullName;
    private String otherUserAvatar;
    private String lastMessage;
    private Long lastSenderId;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
}
