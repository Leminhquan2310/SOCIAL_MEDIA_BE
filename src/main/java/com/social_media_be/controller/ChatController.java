package com.social_media_be.controller;

import com.social_media_be.dto.chat.ConversationResponseDto;
import com.social_media_be.dto.chat.MessageDto;
import com.social_media_be.dto.chat.ChatRequest;
import com.social_media_be.entity.User;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.service.ChatService;
import com.social_media_be.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // WebSocket: Gửi tin nhắn
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatRequest request) {
        log.info("WebSocket request to send message from {}", request.getSenderId());
        
        MessageDto savedMessage = chatService.sendMessage(request);
        
        // Fetch all members of this conversation to broadcast
        java.util.List<User> members = chatService.getConversationMembers(savedMessage.getConversationId());

        for (User member : members) {
            // Broadcast tin nhắn
            messagingTemplate.convertAndSendToUser(
                    member.getUsername(),
                    "/queue/messages",
                    savedMessage
            );
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getConversations(currentUser.getId(), pageable)));
    }

    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<?> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessages(conversationId, currentUser.getId(), pageable)));
    }

    @PostMapping("/seen/{conversationId}")
    public ResponseEntity<Void> markAsSeen(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        chatService.markAsSeen(conversationId, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getTotalUnreadCount(currentUser.getId())));
    }
}
