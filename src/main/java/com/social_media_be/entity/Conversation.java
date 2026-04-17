package com.social_media_be.entity;

import com.social_media_be.entity.enums.ConversationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Builder.Default
    private ConversationType type = ConversationType.PRIVATE;

    @Column(name = "title")
    private String title;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_sender_id")
    private Long lastSenderId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ConversationMember> members = new HashSet<>();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (lastMessageAt == null) {
            lastMessageAt = LocalDateTime.now();
        }
    }
}
