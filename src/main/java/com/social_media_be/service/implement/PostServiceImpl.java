package com.social_media_be.service.implement;

import com.social_media_be.dto.post.PostCreateRequest;
import com.social_media_be.dto.post.PostImageDto;
import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.dto.post.PostUpdateRequest;
import com.social_media_be.dto.user.UserSummary;
import com.social_media_be.entity.*;
import com.social_media_be.entity.enums.*;
import com.social_media_be.exception.BadRequestException;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.repository.*;
import com.social_media_be.service.CloudinaryService;
import com.social_media_be.service.ContentModerationService;
import com.social_media_be.service.NotificationService;
import com.social_media_be.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final CloudinaryService cloudinaryService;
    private final LikeRepository likeRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final CommentRepository commentRepository;
    private final PostReportRepository postReportRepository;
    private final ContentModerationService contentModerationService;
    private static final List<String> VIOLATION_KEYWORDS = List.of(
            "nudity",
            "explicit",
            "sexual",
            "violence",
            "weapon",
            "blood"
    );

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Privacy normalizeUserRequestedPrivacyForCreate(Privacy requestedPrivacy) {
        if (requestedPrivacy == null || requestedPrivacy == Privacy.HIDDEN) {
            return Privacy.PUBLIC;
        }
        return requestedPrivacy;
    }

    private Privacy normalizeUserRequestedPrivacyForUpdate(Privacy currentPrivacy, Privacy requestedPrivacy) {
        if (requestedPrivacy == null || requestedPrivacy == Privacy.HIDDEN) {
            return currentPrivacy;
        }
        if (currentPrivacy == Privacy.HIDDEN) {
            return Privacy.HIDDEN;
        }
        return requestedPrivacy;
    }

    private void validateVisiblePostAccess(Post post, Long currentUserId) {
        if (post.getPrivacy() == Privacy.HIDDEN) {
            throw new ResourceNotFoundException("Post not found");
        }

        Long postOwnerId = post.getUser().getId();
        if (currentUserId != null && postOwnerId.equals(currentUserId)) {
            return;
        }

        if (post.getPrivacy() == Privacy.PUBLIC) {
            return;
        }

        if (post.getPrivacy() == Privacy.ONLY_ME) {
            throw new ResourceNotFoundException("Post not found");
        }

        if (post.getPrivacy() == Privacy.FRIEND_ONLY) {
            boolean isFriend = currentUserId != null
                    && friendshipRepository.existsByUsersAndStatus(currentUserId, postOwnerId, FriendStatus.ACCEPTED);
            if (!isFriend) {
                throw new ResourceNotFoundException("Post not found");
            }
        }
    }

    private PostResponse mapToResponse(Post post, Long currentUserId) {
        UserSummary author = UserSummary.builder()
                .id(post.getUser().getId())
                .username(post.getUser().getUsername())
                .fullName(post.getUser().getFullName())
                .avatarUrl(post.getUser().getAvatarUrl())
                .build();

        List<PostImageDto> imageDtos = post.getImages().stream()
                .filter(img -> img.getStatus() == MediaStatus.ACTIVE)
                .sorted((a, b) -> {
                    int idxA = a.getOrderIndex() != null ? a.getOrderIndex() : 0;
                    int idxB = b.getOrderIndex() != null ? b.getOrderIndex() : 0;
                    return Integer.compare(idxA, idxB);
                })
                .map(img -> PostImageDto.builder()
                        .id(img.getId())
                        .mediaUrl(img.getMediaUrl())
                        .mediaType(img.getMediaType())
                        .orderIndex(img.getOrderIndex())
                        .status(img.getStatus()) // Added status for potential frontend usage
                        .build())
                .collect(Collectors.toList());

        boolean isLiked = currentUserId != null &&
                likeRepository.existsByUserIdAndTargetIdAndTargetType(currentUserId, post.getId(), TargetType.POST);

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .privacy(post.getPrivacy())
                .feeling(post.getFeeling())
                .author(author)
                .images(imageDtos)
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : 0)
                .isLiked(isLiked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public PostResponse createPost(PostCreateRequest request, Long userId) {
        contentModerationService.validateContent(request.getContent());
        User user = getUserById(userId);

        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .privacy(normalizeUserRequestedPrivacyForCreate(request.getPrivacy()))
                .feeling(request.getFeeling())
                .images(new ArrayList<>())
                .build();

        int order = 0;
        // Process Images
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (MultipartFile file : request.getImages()) {
                processMediaUpload(post, file, MediaType.IMAGE, order++);
            }
        }

        // Process Videos
        if (request.getVideos() != null && !request.getVideos().isEmpty()) {
            for (MultipartFile file : request.getVideos()) {
                processMediaUpload(post, file, MediaType.VIDEO, order++);
            }
        }

        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost, userId);
    }

    private void processMediaUpload(Post post, MultipartFile file, MediaType type, int order) {
        try {
            String resourceType = type == MediaType.VIDEO ? "video" : "image";
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinaryService.upload(file,
                    "social-media/posts", resourceType, true);

            String mediaUrl = (String) uploadResult.get("secure_url");
            double violationScore = extractViolationScore(uploadResult);
            
            MediaStatus status = MediaStatus.ACTIVE;
            if (violationScore >= 0.8) {
                status = MediaStatus.REJECTED;
            } else if (violationScore >= 0.5) {
                status = MediaStatus.FLAGGED;
            }

            PostImage postMedia = PostImage.builder()
                    .post(post)
                    .mediaUrl(mediaUrl)
                    .mediaType(type)
                    .orderIndex(order)
                    .violationScore(violationScore)
                    .status(status)
                    .build();

            post.getImages().add(postMedia);
        } catch (IOException ioe) {
            log.error("Cannot upload {} to Cloudinary!", type, ioe);
            throw new RuntimeException("Upload failed: " + file.getOriginalFilename());
        }
    }

    private double extractViolationScore(Map<String, Object> uploadResult) {
        try {
            List<Map<String, Object>> moderation = (List<Map<String, Object>>) uploadResult.get("moderation");

            if (moderation == null || moderation.isEmpty()) return 0.0;

            double maxViolationScore = 0.0;
            double maxFallbackScore = 0.0;

            for (Map<String, Object> mod : moderation) {

                String kind = (String) mod.get("kind");
                if (!"aws_rek".equals(kind) && !"google_vision".equals(kind)) continue;

                Map<String, Object> response = (Map<String, Object>) mod.get("response");

                if (response == null) continue;

                // AWS Rekognition
                List<Map<String, Object>> labels = (List<Map<String, Object>>) response.get("moderation_labels");

                // fallback Google Vision or other format
                if (labels == null) {
                    labels = (List<Map<String, Object>>) response.get("labels");
                }
                
                // Cloudinary documentation sometimes uses "ModerationLabels" capitalization
                if (labels == null) {
                    labels = (List<Map<String, Object>>) response.get("ModerationLabels");
                }

                if (labels == null) continue;

                for (Map<String, Object> label : labels) {

                    String name = (String) label.get("name");
                    Object confidenceObj = label.get("confidence");

                    if (!(confidenceObj instanceof Number)) continue;

                    double confidence = ((Number) confidenceObj).doubleValue() / 100.0;

                    // 👉 log để debug + học dữ liệu thật
                    log.debug("AI label: {}, confidence: {}", name, confidence);

                    if (name == null) continue;

                    String lower = name.toLowerCase();

                    // 🔥 Rule chính: match keyword nguy hiểm
                    boolean isViolation = VIOLATION_KEYWORDS.stream()
                            .anyMatch(lower::contains);

                    if (isViolation) {
                        maxViolationScore = Math.max(maxViolationScore, confidence);
                    }

                    // 🔥 fallback: lưu max tất cả label
                    maxFallbackScore = Math.max(maxFallbackScore, confidence);
                }
            }

            // 🎯 ưu tiên score từ label nguy hiểm
            if (maxViolationScore > 0) {
                return maxViolationScore;
            }

            // 🎯 fallback nếu không match label nhưng score rất cao
            if (maxFallbackScore > 0.9) {
                return maxFallbackScore;
            }

        } catch (Exception e) {
            log.warn("Failed to extract violation score from moderation response", e);
        }

        return 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts(Long userId, Long lastPostId, int limit) {
        User user = getUserById(userId);
        Pageable pageable = PageRequest.of(0, limit);
        List<Post> posts = postRepository.findUserPosts(user.getId(), lastPostId, pageable);
        return posts.stream().map(p -> mapToResponse(p, userId)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request, Long userId) {
        User user = getUserById(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to edit this post");
        }

        if (request.getContent() != null) {
            contentModerationService.validateContent(request.getContent());
        }

        post.setContent(request.getContent() != null ? request.getContent() : post.getContent());
        if (request.getPrivacy() != null) {
            post.setPrivacy(normalizeUserRequestedPrivacyForUpdate(post.getPrivacy(), request.getPrivacy()));
        }
        post.setFeeling(request.getFeeling() != null ? request.getFeeling() : post.getFeeling());

        if (request.getDeletedImageIds() != null && !request.getDeletedImageIds().isEmpty()) {
            List<PostImage> toDelete = post.getImages().stream()
                    .filter(img -> request.getDeletedImageIds().contains(img.getId()))
                    .collect(Collectors.toList());
            for (PostImage img : toDelete) {
                try {
                    String publicId = cloudinaryService.extractPublicId(img.getMediaUrl());
                    if (publicId != null)
                        cloudinaryService.delete(publicId);
                } catch (Exception e) {
                    log.error("Failed to delete media from Cloudinary: {}", img.getMediaUrl(), e);
                }
                post.getImages().remove(img);
            }
        }

        int currentMaxOrder = post.getImages().stream()
                .mapToInt(img -> img.getOrderIndex() != null ? img.getOrderIndex() : 0)
                .max().orElse(-1);

        if (request.getNewImages() != null && !request.getNewImages().isEmpty()) {
            for (MultipartFile file : request.getNewImages()) {
                validateMedia(file, MediaType.IMAGE);
                processMediaUpload(post, file, MediaType.IMAGE, ++currentMaxOrder);
            }
        }

        if (request.getNewVideos() != null && !request.getNewVideos().isEmpty()) {
            for (MultipartFile file : request.getNewVideos()) {
                validateMedia(file, MediaType.VIDEO);
                processMediaUpload(post, file, MediaType.VIDEO, ++currentMaxOrder);
            }
        }

        // Handle Reordering and New Image Integration
        if (request.getImageOrder() != null && !request.getImageOrder().isEmpty()) {
            List<PostImage> currentImages = post.getImages();

            // To track which new image is which: filter those without ID (newly added at
            // line 171)
            List<PostImage> newAddedImages = currentImages.stream()
                    .filter(img -> img.getId() == null)
                    .collect(Collectors.toList());

            // Build the final set of images that should remain
            List<PostImage> orderedList = new ArrayList<>();
            for (int i = 0; i < request.getImageOrder().size(); i++) {
                String ref = request.getImageOrder().get(i);
                final int finalI = i;

                if (ref.startsWith("new-")) {
                    try {
                        int subIdx = Integer.parseInt(ref.substring(4));
                        if (subIdx < newAddedImages.size()) {
                            PostImage img = newAddedImages.get(subIdx);
                            img.setOrderIndex(finalI);
                            orderedList.add(img);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    try {
                        Long id = Long.parseLong(ref);
                        currentImages.stream()
                                .filter(img -> img.getId() != null && img.getId().equals(id))
                                .findFirst()
                                .ifPresent(img -> {
                                    img.setOrderIndex(finalI);
                                    orderedList.add(img);
                                });
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Safety check: Keep any existing images that weren't in the ordering list but
            // weren't marked for deletion
            for (PostImage img : currentImages) {
                if (!orderedList.contains(img)) {
                    img.setOrderIndex(orderedList.size());
                    orderedList.add(img);
                }
            }

            // Robust collection update for JPA orphanRemoval
            // Instead of setImages(newList), we modify the existing managed collection
            List<PostImage> toRemove = currentImages.stream()
                    .filter(img -> !orderedList.contains(img))
                    .collect(Collectors.toList());
            currentImages.removeAll(toRemove);

            // Add any that are in orderedList but not in currentImages (should be none
            // because we added them all at 171)
            // But we actually want to ensure the orderIndex is updated. The objects are the
            // same, so it's already updated.
        }

        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost, userId);
    }

    private void validateMedia(MultipartFile file, MediaType type) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException(type + " file is empty");
        }
        String contentType = file.getContentType();
        if (type == MediaType.IMAGE) {
            if (contentType == null || (!contentType.equals("image/jpeg") &&
                    !contentType.equals("image/png") &&
                    !contentType.equals("image/webp"))) {
                throw new IllegalArgumentException("Invalid image format. Only JPG, PNG, and WEBP are allowed.");
            }
        } else {
            if (contentType == null || (!contentType.startsWith("video/"))) {
                throw new IllegalArgumentException("Invalid video format.");
            }
            if (file.getSize() > 100 * 1024 * 1024) {
                throw new IllegalArgumentException("Video size exceeds 100MB limit.");
            }
        }
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        User user = getUserById(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // check quyền xóa
        if (!post.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to delete this post");
        }

        // 1. Thu thập tất cả comment để dọn dẹp
        List<Comment> comments = commentRepository.findAllByPostId(postId);
        List<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());

        // 2. Xóa ảnh của các comment trên Cloudinary
        for (Comment comment : comments) {
            if (comment.getMediaUrl() != null) {
                try {
                    String publicId = cloudinaryService.extractPublicId(comment.getMediaUrl());
                    if (publicId != null)
                        cloudinaryService.delete(publicId);
                } catch (Exception e) {
                    log.error("Failed to delete comment media from Cloudinary: {}", comment.getMediaUrl(), e);
                }
            }
        }

        // 3. Xóa ảnh của bài viết trên Cloudinary
        for (PostImage img : post.getImages()) {
            try {
                String publicId = cloudinaryService.extractPublicId(img.getMediaUrl());
                if (publicId != null)
                    cloudinaryService.delete(publicId);
            } catch (Exception e) {
                log.error("Failed to delete post media from Cloudinary: {}", img.getMediaUrl(), e);
            }
        }

        // 4. Xóa Lượt thích (Likes)
        // Xóa like của bài viết
        likeRepository.deleteByTargetIdAndTargetType(postId, TargetType.POST);
        // Xóa like của tất cả comment thuộc bài viết
        if (!commentIds.isEmpty()) {
            likeRepository.deleteByTargetIdInAndTargetType(commentIds, TargetType.COMMENT);
        }

        // 5. Xóa Thông báo (Notifications)
        // Xóa thông báo liên quan đến bài viết (LIKE_POST, COMMENT_POST)
        notificationService.removeNotificationByPostId(postId);
        // Xóa thông báo liên quan đến các comment (LIKE_COMMENT, REPLY_COMMENT)
        if (!commentIds.isEmpty()) {
            notificationRepository.deleteByReferenceIdInAndTypeIn(
                    commentIds,
                    List.of(NotificationType.LIKE_COMMENT, NotificationType.REPLY_COMMENT));
        }

        // 6. Xóa dữ liệu trong DB
        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getNewsFeed(Long userId, Long lastPostId, int limit) {
        User user = getUserById(userId);
        Pageable pageable = PageRequest.of(0, limit);
        List<Post> posts = postRepository.findNewsFeedPosts(user.getId(), lastPostId, pageable);
        return posts.stream().map(p -> mapToResponse(p, userId)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getUserPosts(Long targetUserId, Long currentUserId, Long lastPostId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            return getMyPosts(currentUserId, lastPostId, limit);
        }

        boolean isFriend = currentUserId != null
                && friendshipRepository.existsByUsersAndStatus(currentUserId, targetUserId, FriendStatus.ACCEPTED);
        List<Privacy> allowedPrivacies = isFriend ? List.of(Privacy.PUBLIC, Privacy.FRIEND_ONLY)
                : List.of(Privacy.PUBLIC);

        List<Post> posts = postRepository.findUserPostsWithPrivacy(targetUserId, allowedPrivacies, lastPostId,
                pageable);
        return posts.stream().map(p -> mapToResponse(p, currentUserId)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> searchPosts(String keyword, Long userId, Long lastPostId, int limit) {
        User user = getUserById(userId);
        Pageable pageable = PageRequest.of(0, limit);
        List<Post> posts = postRepository.searchFeedPosts(user.getId(), keyword, lastPostId, pageable);
        return posts.stream().map(p -> mapToResponse(p, userId)).collect(Collectors.toList());
    }

    @Override
    public PostResponse getPostById(Long postId, Long currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post id not found"));
        validateVisiblePostAccess(post, currentUser);
        return mapToResponse(post, currentUser);
    }

    @Override
    @Transactional
    public void reportPost(Long postId, Long userId, com.social_media_be.dto.post.PostReportRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        User reporter = getUserById(userId);

        if (post.getPrivacy() == Privacy.HIDDEN) {
            throw new BadRequestException("Cannot report a hidden post");
        }

        if (postReportRepository.existsByReporterIdAndPostId(userId, postId)) {
            throw new IllegalStateException("You have already reported this post");
        }

        PostReport report = PostReport.builder()
                .post(post)
                .reporter(reporter)
                .reason(request.getReason())
                .build();

        postReportRepository.save(report);

        post.setReportCount((post.getReportCount() != null ? post.getReportCount() : 0) + 1);
        postRepository.save(post);
    }
}
