package com.social_media_be.service;

import com.social_media_be.dto.chat.ConversationResponseDto;
import com.social_media_be.dto.chat.MessageDto;
import com.social_media_be.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import com.social_media_be.dto.chat.ChatRequest;

public interface ChatService {
    MessageDto sendMessage(ChatRequest request);
    List<Long> getConversationMemberIds(Long conversationId);
    List<User> getConversationMembers(Long conversationId);
    Page<ConversationResponseDto> getConversations(Long userId, Pageable pageable);
    Page<MessageDto> getMessages(Long conversationId, Long userId, Pageable pageable);
    void markAsSeen(Long conversationId, Long userId);
    Long getTotalUnreadCount(Long userId);
    ConversationResponseDto getOrCreateConversation(Long userId, Long receiverId);
}
