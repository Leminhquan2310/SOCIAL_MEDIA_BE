package com.social_media_be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message_mentions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"message_id", "user_id"})
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageMention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
