package com.social_media_be.service;

import com.social_media_be.dto.notification.NotificationResponse;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void createAndSendNotification(User receiver, User actor, NotificationType type, Long referenceId);
    Page<NotificationResponse> getNotifications(Long userId, Pageable pageable);
    long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
}
