package com.social_media_be.repository;

import com.social_media_be.entity.Post;
import com.social_media_be.entity.enums.Privacy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

       // Lấy bài viết của chính mình
       @Query("SELECT p FROM Post p WHERE p.user.id = :userId AND p.user.enabled = true AND p.privacy <> 'HIDDEN' AND (:lastPostId IS NULL OR p.id < :lastPostId) ORDER BY p.createdAt DESC")
       List<Post> findUserPosts(@Param("userId") Long userId, @Param("lastPostId") Long lastPostId, Pageable pageable);

       // Lấy bài viết của một user khác kèm theo filter Privacy
       @Query("SELECT p FROM Post p WHERE p.user.id = :userId AND p.user.enabled = true AND p.privacy <> 'HIDDEN' AND p.privacy IN :privacies AND (:lastPostId IS NULL OR p.id < :lastPostId) ORDER BY p.createdAt DESC")
       List<Post> findUserPostsWithPrivacy(@Param("userId") Long userId, @Param("privacies") List<Privacy> privacies,
                     @Param("lastPostId") Long lastPostId, Pageable pageable);

       // Lấy Newsfeed
       @Query("SELECT p FROM Post p WHERE " +
                     "p.user.enabled = true AND " +
                     "p.privacy <> 'HIDDEN' AND " +
                     "(:lastPostId IS NULL OR p.id < :lastPostId) AND " +
                     "(p.user.id = :userId OR " +
                     " p.privacy = 'PUBLIC' OR " +
                     " (p.privacy = 'FRIEND_ONLY' AND ( " +
                     "   p.user.id IN (SELECT f.receiver.id FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'ACCEPTED') OR "
                     +
                     "   p.user.id IN (SELECT f.requester.id FROM Friendship f WHERE f.receiver.id = :userId AND f.status = 'ACCEPTED') "
                     +
                     " ))) " +
                     "ORDER BY p.createdAt DESC")
       List<Post> findNewsFeedPosts(@Param("userId") Long userId, @Param("lastPostId") Long lastPostId,
                     Pageable pageable);

       // Tìm kiếm toàn cục tương tự điều kiện Newsfeed
       @Query("SELECT p FROM Post p WHERE " +
                     "p.user.enabled = true AND " +
                     "p.privacy <> 'HIDDEN' AND " +
                     "(:lastPostId IS NULL OR p.id < :lastPostId) AND " +
                     "(LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
                     "(p.user.id = :userId OR " +
                     " p.privacy = 'PUBLIC' OR " +
                     " (p.privacy = 'FRIEND_ONLY' AND ( " +
                     "   p.user.id IN (SELECT f.receiver.id FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'ACCEPTED') OR "
                     +
                     "   p.user.id IN (SELECT f.requester.id FROM Friendship f WHERE f.receiver.id = :userId AND f.status = 'ACCEPTED') "
                     +
                     " ))) " +
                     "ORDER BY p.createdAt DESC")
       List<Post> searchFeedPosts(@Param("userId") Long userId, @Param("keyword") String keyword,
                     @Param("lastPostId") Long lastPostId, Pageable pageable);

       @Transactional
       @Modifying
       @Query("UPDATE Post p SET p.likeCount = GREATEST(0, COALESCE(p.likeCount, 0) + :delta) WHERE p.id = :postId")
       void incrementLikeCount(@Param("postId") Long postId, @Param("delta") long delta);

       @Transactional
       @Modifying
       @Query("UPDATE Post p SET p.likeCount = GREATEST(0, COALESCE(p.likeCount, 0) - 1) WHERE p.id = :postId")
       void decrementLikeCount(@Param("postId") Long postId);

       @Query("SELECT COUNT(p.id) FROM Post p WHERE p.id = :postId")
       Long getLikeCount(@Param("postId") Long postId);

       @Transactional
       @Modifying
       @Query("UPDATE Post p SET p.commentCount = COALESCE(p.commentCount, 0) + 1 WHERE p.id = :postId")
       void incrementCommentCount(@Param("postId") Long postId);

       @Transactional
       @Modifying
       @Query("UPDATE Post p SET p.commentCount = GREATEST(0, COALESCE(p.commentCount, 0) - :amount) WHERE p.id = :postId")
       void decrementCommentCount(@Param("postId") Long postId, @Param("amount") long amount);
}
