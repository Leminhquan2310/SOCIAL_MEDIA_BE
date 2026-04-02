package com.social_media_be.service;

import com.social_media_be.dto.comment.CommentRequestDto;
import com.social_media_be.dto.comment.CommentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {
    CommentResponseDto createComment(Long postId, CommentRequestDto request, Long userId);
    CommentResponseDto updateComment(Long commentId, CommentRequestDto request, Long userId);
    void deleteComment(Long commentId, Long userId);
    Page<CommentResponseDto> getCommentsByPost(Long postId, Long userId, Pageable pageable);
    List<CommentResponseDto> getReplies(Long parentCommentId, Long userId);
}
