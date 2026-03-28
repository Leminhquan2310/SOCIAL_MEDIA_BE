package com.social_media_be.dto.notification;

import com.social_media_be.dto.user.UserSummary;
import com.social_media_be.entity.Notification;
import com.social_media_be.entity.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class NotificationResponse {
    private Long id;
    private UserSummary actor;
    private NotificationType type;
    private Long referenceId;
    private boolean isRead;
    private boolean isActionable;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .actor(UserSummary.builder()
                        .id(notification.getActor().getId())
                        .username(notification.getActor().getUsername())
                        .fullName(notification.getActor().getFullName())
                        .avatarUrl(notification.getActor().getAvatarUrl())
                        .build())
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.isRead())
                .isActionable(false) // Will be set by service if needed
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
