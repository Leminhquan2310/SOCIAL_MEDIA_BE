package com.social_media_be.dto.chat;

import lombok.Data;

@Data
public class ChatRequest {
    private Long senderId;
    private Long receiverId; // Optional for existing conversations
    private Long conversationId; // Optional for first-time private messages
    private String content;
}
