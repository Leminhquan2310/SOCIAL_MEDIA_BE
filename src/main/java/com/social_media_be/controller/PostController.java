package com.social_media_be.controller;

import com.social_media_be.dto.post.PostCreateRequest;
import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.dto.post.PostUpdateRequest;
import com.social_media_be.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.social_media_be.entity.UserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@ModelAttribute PostCreateRequest request, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PostResponse response = postService.createPost(request, userPrincipal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<PostResponse>> getMyPosts(
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<PostResponse> response = postService.getMyPosts(userPrincipal.getId(), lastPostId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PostResponse response = postService.getPostById(postId, userPrincipal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            @ModelAttribute PostUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PostResponse response = postService.updatePost(postId, request, userPrincipal.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        postService.deletePost(postId, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> getNewsFeed(
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<PostResponse> response = postService.getNewsFeed(userPrincipal.getId(), lastPostId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long currentUserId = userPrincipal != null ? userPrincipal.getId() : null;
        List<PostResponse> response = postService.getUserPosts(userId, currentUserId, lastPostId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostResponse>> searchPosts(
            @RequestParam String q,
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<PostResponse> response = postService.searchPosts(q, userPrincipal.getId(), lastPostId, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/report")
    public ResponseEntity<Void> reportPost(
            @PathVariable Long postId,
            @RequestBody com.social_media_be.dto.post.PostReportRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        postService.reportPost(postId, userPrincipal.getId(), request);
        return ResponseEntity.ok().build();
    }
}
