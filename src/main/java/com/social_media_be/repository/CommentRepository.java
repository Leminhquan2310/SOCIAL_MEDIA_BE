package com.social_media_be.repository;

import com.social_media_be.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Tìm các comment gốc (không có parent) của 1 bài viết, hỗ trợ phân trang
    Page<Comment> findByPostIdAndParentCommentIsNullOrderByCreatedAtDesc(Long postId, Pageable pageable);
    
    // Tìm các comment gốc theo cursor (nếu muốn dùng cursor pagination)
    List<Comment> findByPostIdAndParentCommentIsNullOrIdLessThanOrderByCreatedAtDesc(Long postId, Long lastId, Pageable pageable);

    // Lấy tất cả reply của 1 comment gốc (xếp theo thời gian tăng dần)
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    // Đếm số reply của 1 comment gốc (tránh N+1)
    long countByParentCommentId(Long parentCommentId);

    // Đếm tổng số lượng comment của 1 post
    long countByPostId(Long postId);
}
