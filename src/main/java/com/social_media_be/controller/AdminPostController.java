package com.social_media_be.controller;

import com.social_media_be.dto.admin.AdminPostResponseDto;
import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.entity.enums.Privacy;
import com.social_media_be.service.AdminPostService;
import com.social_media_be.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final AdminPostService adminPostService;

    @GetMapping
    public ResponseEntity<?> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Privacy status,
            @RequestParam(required = false) Integer minReports,
            @RequestParam(required = false) Integer maxReports) {
        Sort sort = Sort.by(direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminPostResponseDto> posts = adminPostService.getAllPosts(pageable, username, status, minReports,
                maxReports);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostById(@PathVariable Long postId) {
        PostResponse post = adminPostService.getPostById(postId);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @GetMapping("/{postId}/reports")
    public ResponseEntity<?> getPostReports(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(adminPostService.getPostReports(postId)));
    }

    @PatchMapping("/{postId}/hide")
    public ResponseEntity<?> hidePost(@PathVariable Long postId) {
        adminPostService.hidePost(postId);
        return ResponseEntity.ok(ApiResponse.success("Bài viết đã được ẩn"));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        adminPostService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.success("Bài viết đã được xóa"));
    }

    @DeleteMapping("/{postId}/reports")
    public ResponseEntity<?> dismissReports(@PathVariable Long postId) {
        adminPostService.dismissReports(postId);
        return ResponseEntity.ok(ApiResponse.success("Đã gỡ bỏ các báo cáo cho bài viết này"));
    }
}
