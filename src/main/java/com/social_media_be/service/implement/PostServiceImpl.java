package com.social_media_be.service.implement;

import com.social_media_be.dto.post.PostCreateRequest;
import com.social_media_be.dto.post.PostImageDto;
import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.dto.post.PostUpdateRequest;
import com.social_media_be.dto.user.UserSummary;
import com.social_media_be.entity.Post;
import com.social_media_be.entity.PostImage;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.FriendStatus;
import com.social_media_be.entity.enums.Privacy;
import com.social_media_be.entity.enums.TargetType;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.repository.FriendshipRepository;
import com.social_media_be.repository.LikeRepository;
import com.social_media_be.repository.PostRepository;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.CloudinaryService;
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

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PostResponse mapToResponse(Post post, Long currentUserId) {
        UserSummary author = UserSummary.builder()
                .id(post.getUser().getId())
                .username(post.getUser().getUsername())
                .fullName(post.getUser().getFullName())
                .avatarUrl(post.getUser().getAvatarUrl())
                .build();

        List<PostImageDto> imageDtos = post.getImages().stream()
                .sorted((a, b) -> {
                    int idxA = a.getOrderIndex() != null ? a.getOrderIndex() : 0;
                    int idxB = b.getOrderIndex() != null ? b.getOrderIndex() : 0;
                    return Integer.compare(idxA, idxB);
                })
                .map(img -> PostImageDto.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .orderIndex(img.getOrderIndex())
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
    public PostResponse createPost(PostCreateRequest request, Long userId){
        User user = getUserById(userId);

        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .privacy(request.getPrivacy() != null ? request.getPrivacy() : Privacy.PUBLIC)
                .feeling(request.getFeeling())
                .images(new ArrayList<>())
                .build();

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            int order = 0;
            try {
                for (MultipartFile file : request.getImages()) {
                    Map<String, Object> uploadResult = (Map<String, Object>) cloudinaryService.upload(file, "social-media/posts");
                    String imgUrl = (String) uploadResult.get("secure_url");
                    PostImage postImage = PostImage.builder()
                            .post(post)
                            .imageUrl(imgUrl)
                            .orderIndex(order++)
                            .build();
                    post.getImages().add(postImage);
                }
            } catch (IOException ioe) {
                log.error("Can not upload file!");
                ioe.printStackTrace();
            }
        }

        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost, userId);
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

        post.setContent(request.getContent() != null ? request.getContent() : post.getContent());
        post.setPrivacy(request.getPrivacy() != null ? request.getPrivacy() : post.getPrivacy());
        post.setFeeling(request.getFeeling() != null ? request.getFeeling() : post.getFeeling());

        if (request.getDeletedImageIds() != null && !request.getDeletedImageIds().isEmpty()) {
            List<PostImage> toDelete = post.getImages().stream()
                    .filter(img -> request.getDeletedImageIds().contains(img.getId()))
                    .collect(Collectors.toList());
            for (PostImage img : toDelete) {
                try {
                    String publicId = cloudinaryService.extractPublicId(img.getImageUrl());
                    if (publicId != null) cloudinaryService.delete(publicId);
                } catch (Exception e) {
                    log.error("Failed to delete image from Cloudinary: {}", img.getImageUrl(), e);
                }
                post.getImages().remove(img);
            }
        }

        if (request.getNewImages() != null && !request.getNewImages().isEmpty()) {
            List<PostImage> newlyUploaded = new ArrayList<>();
            for (MultipartFile file : request.getNewImages()) {
                validateImage(file);
                try {
                    Map<?, ?> uploadResult = cloudinaryService.upload(file, "social-media/posts");
                    String imgUrl = (String) uploadResult.get("secure_url");
                    PostImage postImage = PostImage.builder()
                            .post(post)
                            .imageUrl(imgUrl)
                            .orderIndex(0) // Will be updated by reordering logic
                            .build();
                    newlyUploaded.add(postImage);
                } catch (IOException ioe) {
                    log.error("Can not upload file!");
                    throw new RuntimeException("Failed to upload image", ioe);
                }
            }
            post.getImages().addAll(newlyUploaded);
        }

        // Handle Reordering and New Image Integration
        if (request.getImageOrder() != null && !request.getImageOrder().isEmpty()) {
            List<PostImage> currentImages = post.getImages();
            
            // To track which new image is which: filter those without ID (newly added at line 171)
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
                    } catch (NumberFormatException ignored) {}
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
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // Safety check: Keep any existing images that weren't in the ordering list but weren't marked for deletion
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
            
            // Add any that are in orderedList but not in currentImages (should be none because we added them all at 171)
            // But we actually want to ensure the orderIndex is updated. The objects are the same, so it's already updated.
        }

        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost, userId);
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && 
                                   !contentType.equals("image/png") && 
                                   !contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Invalid image format. Only JPG, PNG, and WEBP are allowed.");
        }
        // Optional: check file extension too
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches("(?i).*\\.(jpg|jpeg|png|webp)$")) {
            throw new IllegalArgumentException("Invalid image file extension.");
        }
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        User user = getUserById(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to delete this post");
        }

        for (PostImage img : post.getImages()) {
            try {
                String publicId = cloudinaryService.extractPublicId(img.getImageUrl());
                if (publicId != null) cloudinaryService.delete(publicId);
            } catch (Exception e) {
                log.error("Failed to delete image from Cloudinary: {}", img.getImageUrl(), e);
            }
        }

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

        boolean isFriend = currentUserId != null && friendshipRepository.existsByUsersAndStatus(currentUserId, targetUserId, FriendStatus.ACCEPTED);
        List<Privacy> allowedPrivacies = isFriend ? List.of(Privacy.PUBLIC, Privacy.FRIEND_ONLY) : List.of(Privacy.PUBLIC);

        List<Post> posts = postRepository.findUserPostsWithPrivacy(targetUserId, allowedPrivacies, lastPostId, pageable);
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
}
