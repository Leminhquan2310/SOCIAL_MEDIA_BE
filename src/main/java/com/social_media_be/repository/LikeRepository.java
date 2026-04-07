package com.social_media_be.repository;

import com.social_media_be.entity.Like;
import com.social_media_be.entity.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);
    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);
    long countByTargetIdAndTargetType(Long targetId, TargetType targetType);
    
    @Modifying
    @Query("DELETE FROM Like l WHERE l.targetId = :targetId AND l.targetType = :targetType")
    void deleteByTargetIdAndTargetType(@Param("targetId") Long targetId, @Param("targetType") TargetType targetType);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.targetId IN :targetIds AND l.targetType = :targetType")
    void deleteByTargetIdInAndTargetType(@Param("targetIds") Collection<Long> targetIds, @Param("targetType") TargetType targetType);
    
    // Tìm người like mới nhất để làm actor đại diện trong thông báo gộp
    java.util.Optional<Like> findFirstByTargetIdAndTargetTypeOrderByCreatedAtDesc(Long targetId, TargetType targetType);
}
