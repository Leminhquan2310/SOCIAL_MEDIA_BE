package com.social_media_be.repository;

import com.social_media_be.entity.Like;
import com.social_media_be.entity.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);
    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);
    long countByTargetIdAndTargetType(Long targetId, TargetType targetType);
    void deleteByTargetIdAndTargetType(Long targetId, TargetType targetType);
}
