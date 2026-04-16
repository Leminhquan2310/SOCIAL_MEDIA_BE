package com.social_media_be.service.implement;

import com.social_media_be.dto.chat.ConversationResponseDto;
import com.social_media_be.dto.chat.MessageDto;
import com.social_media_be.entity.Conversation;
import com.social_media_be.entity.ConversationMember;
import com.social_media_be.entity.Message;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.ConversationType;
import com.social_media_be.entity.enums.MemberRole;
import com.social_media_be.entity.enums.MessageStatus;
import com.social_media_be.repository.ConversationMemberRepository;
import com.social_media_be.repository.ConversationRepository;
import com.social_media_be.repository.MessageRepository;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.social_media_be.dto.chat.ChatRequest;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MessageDto sendMessage(ChatRequest request) {
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        Conversation conversation;

        if (request.getConversationId() != null) {
            // Existing conversation (Private or Group)
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            // Verify sender is a member
            conversationMemberRepository.findByConversationAndUser(conversation, sender)
                    .orElseThrow(() -> new RuntimeException("Sender is not a member of this conversation"));

        } else if (request.getReceiverId() != null) {
            // Finding or creating Private conversation
            User receiver = userRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new RuntimeException("Receiver not found"));

            conversation = conversationRepository.findPrivateBetweenUsers(sender, receiver)
                    .orElseGet(() -> {
                        Conversation newConv = Conversation.builder()
                                .type(ConversationType.PRIVATE)
                                .build();
                        Conversation savedConv = conversationRepository.save(newConv);

                        ConversationMember member1 = ConversationMember.builder()
                                .conversation(savedConv)
                                .user(sender)
                                .role(MemberRole.MEMBER)
                                .build();
                        ConversationMember member2 = ConversationMember.builder()
                                .conversation(savedConv)
                                .user(receiver)
                                .role(MemberRole.MEMBER)
                                .build();

                        conversationMemberRepository.save(member1);
                        conversationMemberRepository.save(member2);
                        
                        return savedConv;
                    });
        } else {
            throw new RuntimeException("Either conversationId or receiverId must be provided");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .status(MessageStatus.SENT)
                .build();

        Message savedMessage = messageRepository.save(message);

        // Update conversation summary
        conversation.setLastMessage(request.getContent());
        conversation.setLastSenderId(sender.getId());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Increment unread count for all members EXCEPT the sender
        List<ConversationMember> allMembers = conversationMemberRepository.findByConversation(conversation);
        for (ConversationMember member : allMembers) {
            if (!member.getUser().getId().equals(sender.getId())) {
                member.setUnreadCount(member.getUnreadCount() + 1);
            }
        }
        conversationMemberRepository.saveAll(allMembers);

        return mapToMessageDto(savedMessage);
    }

    @Override
    public List<Long> getConversationMemberIds(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return conversationMemberRepository.findByConversation(conversation)
                .stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<User> getConversationMembers(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return conversationMemberRepository.findByConversation(conversation)
                .stream()
                .map(member -> {
                    User user = member.getUser();
                    org.hibernate.Hibernate.initialize(user); // Force initialize proxy
                    return user;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<ConversationResponseDto> getConversations(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return conversationRepository.findAllByUser(user, pageable)
                .map(conv -> mapToConversationResponseDto(conv, user));
    }

    @Override
    public Page<MessageDto> getMessages(Long conversationId, Long userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Ensure user belongs to conversation
        conversationMemberRepository.findByConversationAndUser(conversation, user)
                .orElseThrow(() -> new RuntimeException("Unauthorized access to conversation"));

        return messageRepository.findAllByConversationOrderByCreatedAtDesc(conversation, pageable)
                .map(this::mapToMessageDto);
    }

    @Override
    @Transactional
    public void markAsSeen(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
                
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ConversationMember member = conversationMemberRepository.findByConversationAndUser(conversation, user)
                .orElseThrow(() -> new RuntimeException("Unauthorized access to conversation"));

        // Mark messages as seen
        messageRepository.markAllAsSeen(conversation, userId, MessageStatus.SEEN, LocalDateTime.now());

        // Reset unread count for the current user
        member.setUnreadCount(0);
        conversationMemberRepository.save(member);
    }

    @Override
    public Long getTotalUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long count = conversationMemberRepository.countTotalUnread(user);
        return count != null ? count : 0L;
    }

    private MessageDto mapToMessageDto(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .content(message.getContent())
                .status(message.getStatus().name())
                .createdAt(message.getCreatedAt())
                .seenAt(message.getSeenAt())
                .build();
    }

    private ConversationResponseDto mapToConversationResponseDto(Conversation conv, User currentUser) {
        ConversationMember currentUserMember = conversationMemberRepository.findByConversationAndUser(conv, currentUser)
                .orElseThrow(() -> new RuntimeException("User is not a member of this conversation"));
                
        Integer unreadCount = currentUserMember.getUnreadCount();
        
        String displayTitle = conv.getTitle();
        String displayAvatar = conv.getAvatarUrl();
        Long otherUserId = null;
        
        if (conv.getType() == ConversationType.PRIVATE || displayTitle == null) {
            List<ConversationMember> otherMembers = conversationMemberRepository.findOtherMembers(conv, currentUser);
            // Default to first other member if private
            if (!otherMembers.isEmpty()) {
                User otherUser = otherMembers.get(0).getUser();
                displayTitle = displayTitle != null ? displayTitle : otherUser.getFullName();
                displayAvatar = displayAvatar != null ? displayAvatar : otherUser.getAvatarUrl();
                otherUserId = otherUser.getId();
            } else {
                displayTitle = "Unknown User";
            }
        }

        return ConversationResponseDto.builder()
                .id(conv.getId())
                .otherUserId(otherUserId) // In Group, this might be null or meaningless
                .otherUserFullName(displayTitle)
                .otherUserAvatar(displayAvatar)
                .lastMessage(conv.getLastMessage())
                .lastSenderId(conv.getLastSenderId())
                .lastMessageAt(conv.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }
}
