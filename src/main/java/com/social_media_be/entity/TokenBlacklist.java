package com.social_media_be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_token_expires", columnList = "expires_at")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenBlacklist {
    @Id
    @Column(length = 500)
    private String token;

    @Column(name = "banned_at", nullable = false)
    private LocalDateTime bannedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
