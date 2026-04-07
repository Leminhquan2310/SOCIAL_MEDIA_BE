package com.social_media_be.controller;

import com.social_media_be.dto.comment.CommentRequestDto;
import com.social_media_be.dto.comment.CommentResponseDto;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.service.CommentService;
import com.social_media_be.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<?> getCommentsByPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 5) Pageable pageable) {
        Long userId = (userPrincipal != null) ? userPrincipal.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(postId, userId, pageable)));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> createComment(
            @PathVariable Long postId,
            @Valid @ModelAttribute CommentRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(commentService.createComment(postId, request, userPrincipal.getId())));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ApiResponse.success(commentService.updateComment(commentId, request, userPrincipal.getId())));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        commentService.deleteComment(commentId, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<?> getReplies(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = (userPrincipal != null) ? userPrincipal.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(commentService.getReplies(commentId, userId)));
    }
}
