package com.social_media_be.service.implement;

import com.social_media_be.dto.notification.NotificationResponse;
import com.social_media_be.entity.Notification;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.NotificationType;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.exception.UnauthorizedException;
import com.social_media_be.entity.enums.FriendStatus;
import com.social_media_be.repository.FriendshipRepository;
import com.social_media_be.repository.NotificationRepository;
import com.social_media_be.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final FriendshipRepository friendshipRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void createAndSendNotification(User receiver, User actor, NotificationType type, Long referenceId) {
        // 1. Nếu actor và receiver là cùng 1 người, không cần tạo thông báo
        if (receiver.getId().equals(actor.getId())) {
            return;
        }

        // 1.5 Cleanup: Xoá thông báo kết bạn cũ cùng loại giữa 2 người này (Chống spam)
        if (type == NotificationType.FRIEND_REQUEST || type == NotificationType.FRIEND_ACCEPT) {
            notificationRepository.deleteByTypeAndActorIdAndReceiverId(type, actor.getId(), receiver.getId());
        }

        // 2. Lưu vào Database
        Notification notification = Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", receiver.getId(), type);

        // 3. Gửi qua WebSocket
        try {
            log.info("Preparing to send WS notification. Receiver Username: {}, ProviderId: {}", 
                    receiver.getUsername(), receiver.getProviderId());
            messagingTemplate.convertAndSendToUser(
                    receiver.getUsername(),
                    "/queue/notifications",
                    NotificationResponse.fromEntity(saved).toBuilder()
                            .isActionable(saved.getType() == NotificationType.FRIEND_REQUEST)
                            .build()
            );
            log.info("Notification pushed to user {}: /user/queue/notifications", receiver.getUsername());
        } catch (Exception e) {
            log.error("Failed to push notification via WebSocket: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable)
                .map(notification -> {
                    NotificationResponse response = NotificationResponse.fromEntity(notification);
                    
                    // Enhancement Logic: Nếu là thông báo kết bạn, check xem record friendship còn PENDING không
                    if (notification.getType() == NotificationType.FRIEND_REQUEST) {
                        boolean isActionable = friendshipRepository.findById(notification.getReferenceId())
                                .map(f -> f.getStatus() == FriendStatus.PENDING)
                                .orElse(false);
                        return NotificationResponse.builder()
                                .id(response.getId())
                                .actor(response.getActor())
                                .type(response.getType())
                                .referenceId(response.getReferenceId())
                                .isRead(response.isRead())
                                .isActionable(isActionable)
                                .createdAt(response.getCreatedAt())
                                .build();
                    }
                    
                    return response;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedException("You cannot mark this notification as read");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
