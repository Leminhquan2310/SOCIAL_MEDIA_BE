package com.social_media_be.service;

import com.social_media_be.dto.admin.AdminPostResponseDto;
import com.social_media_be.dto.admin.PostReportResponseDto;
import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.entity.enums.Privacy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminPostService {
    Page<AdminPostResponseDto> getAllPosts(
            Pageable pageable,
            String username,
            Privacy privacy,
            Integer minReports,
            Integer maxReports
    );

    List<PostReportResponseDto> getPostReports(Long postId);

    PostResponse getPostById(Long postId);

    void hidePost(Long postId);

    void deletePost(Long postId);

    void dismissReports(Long postId);
}
