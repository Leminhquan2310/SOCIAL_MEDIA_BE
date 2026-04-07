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
    private Long targetId;
    private String ancestorIds;
    private Boolean isRead;
    private Boolean isActionable;
    private Boolean isSilent; // NEW: Nếu true, frontend sẽ không hiện Toast
    private Integer actorCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // Để đồng bộ thời gian mới nhất khi gộp

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
                .targetId(notification.getTargetId())
                .ancestorIds(notification.getAncestorIds())
                .isRead(notification.isRead())
                .isActionable(false)
                .isSilent(false)
                .actorCount(notification.getActorCount())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
