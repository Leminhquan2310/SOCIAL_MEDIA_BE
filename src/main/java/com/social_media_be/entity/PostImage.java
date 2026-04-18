package com.social_media_be.entity;

import com.social_media_be.entity.enums.MediaStatus;
import com.social_media_be.entity.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_images")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    @Builder.Default
    private MediaType mediaType = MediaType.IMAGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MediaStatus status = MediaStatus.ACTIVE;

    @Column(name = "violation_score")
    @Builder.Default
    private Double violationScore = 0.0;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
