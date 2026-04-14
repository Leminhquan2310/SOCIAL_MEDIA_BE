package com.social_media_be.repository;

import com.social_media_be.entity.PostImage;
import com.social_media_be.projection.AdminMediaProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    @Query(value = "SELECT * FROM (" +
            "  SELECT pi.id, pi.media_url as url, pi.media_type as type, pi.status, pi.violation_score as violationScore, " +
            "         'POST' as sourceType, p.id as sourceId, u.full_name as ownerName, pi.created_at as createdAt, p.content " +
            "  FROM post_images pi " +
            "  JOIN posts p ON pi.post_id = p.id " +
            "  JOIN users u ON p.user_id = u.id " +
            "  UNION ALL " +
            "  SELECT c.id, c.media_url as url, c.media_type as type, c.status, c.violation_score as violationScore, " +
            "         'COMMENT' as sourceType, c.id as sourceId, u.full_name as ownerName, c.created_at as createdAt, c.content " +
            "  FROM comments c " +
            "  JOIN users u ON c.user_id = u.id " +
            "  WHERE c.media_url IS NOT NULL" +
            ") AS unified_media " +
            "WHERE (:status IS NULL OR status = :status) " +
            "  AND (:minScore IS NULL OR violationScore >= :minScore) " +
            "ORDER BY createdAt DESC",
            countQuery = "SELECT count(*) FROM (" +
                    "  SELECT pi.id FROM post_images pi " +
                    "  UNION ALL " +
                    "  SELECT c.id FROM comments c WHERE c.media_url IS NOT NULL" +
                    ") AS total",
            nativeQuery = true)
    Page<AdminMediaProjection> findAllUnifiedMedia(
            @Param("status") String status,
            @Param("minScore") Double minScore,
            Pageable pageable);
}
