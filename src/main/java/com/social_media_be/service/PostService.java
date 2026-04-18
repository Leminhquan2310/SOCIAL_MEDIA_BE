package com.social_media_be.service;

import com.social_media_be.dto.post.PostCreateRequest;
import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.dto.post.PostUpdateRequest;

import java.util.List;

public interface PostService {
    PostResponse createPost(PostCreateRequest request, Long userId) ;
    List<PostResponse> getMyPosts(Long userId, Long lastPostId, int limit);
    PostResponse updatePost(Long postId, PostUpdateRequest request, Long userId) ;
    void deletePost(Long postId, Long userId);
    List<PostResponse> getNewsFeed(Long userId, Long lastPostId, int limit);
    List<PostResponse> getUserPosts(Long targetUserId, Long currentUserId, Long lastPostId, int limit);
    List<PostResponse> searchPosts(String keyword, Long userId, Long lastPostId, int limit);
    PostResponse getPostById(Long postId, Long currentUser);
    void reportPost(Long postId, Long userId, com.social_media_be.dto.post.PostReportRequest request);
}
