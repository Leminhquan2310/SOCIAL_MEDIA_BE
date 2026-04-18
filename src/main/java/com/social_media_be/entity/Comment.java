package com.social_media_be.entity;

import com.social_media_be.entity.enums.MediaStatus;
import com.social_media_be.entity.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    @Builder.Default
    private MediaType mediaType = MediaType.IMAGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private MediaStatus status = MediaStatus.ACTIVE;

    @Column(name = "violation_score")
    @Builder.Default
    private Double violationScore = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_user_id")
    private User replyToUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "reply_count")
    @Builder.Default
    private Integer replyCount = 0;

    @Column(name = "is_edited")
    @Builder.Default
    private boolean edited = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
