package com.social_media_be.service.implement;

import com.social_media_be.dto.post.PostCreateRequest;
import com.social_media_be.dto.post.PostImageDto;
import com.social_media_be.dto.post.PostResponse;
import com.social_media_be.dto.post.PostUpdateRequest;
import com.social_media_be.dto.user.UserSummary;
import com.social_media_be.entity.Post;
import com.social_media_be.entity.PostImage;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.AuthProvider;
import com.social_media_be.entity.enums.FriendStatus;
import com.social_media_be.entity.enums.Privacy;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.repository.FriendshipRepository;
import com.social_media_be.repository.PostImageRepository;
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
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final CloudinaryService cloudinaryService;

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PostResponse mapToResponse(Post post) {
        UserSummary author = UserSummary.builder()
                .id(post.getUser().getId())
                .fullName(post.getUser().getFullName())
                .avatarUrl(post.getUser().getAvatarUrl())
                .build();

        List<PostImageDto> imageDtos = post.getImages().stream()
                .map(img -> PostImageDto.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .orderIndex(img.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .privacy(post.getPrivacy())
                .feeling(post.getFeeling())
                .author(author)
                .images(imageDtos)
                .likeCount(0) // TODO: implement likes
                .commentCount(0) // TODO: implement comments
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
                    Map uploadResult = cloudinaryService.upload(file, "social-media/posts");
                    String imgUrl = (String) uploadResult.get("url");
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
        return mapToResponse(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts(Long userId, Long lastPostId, int limit) {
        User user = getUserById(userId);
        Pageable pageable = PageRequest.of(0, limit);
        List<Post> posts = postRepository.findUserPosts(user.getId(), lastPostId, pageable);
        return posts.stream().map(this::mapToResponse).collect(Collectors.toList());
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
            int maxOrder = post.getImages().stream().mapToInt(PostImage::getOrderIndex).max().orElse(-1);
            try {
                for (MultipartFile file : request.getNewImages()) {
                    Map uploadResult = cloudinaryService.upload(file, "social-media/posts");
                    String imgUrl = (String) uploadResult.get("url");
                    PostImage postImage = PostImage.builder()
                            .post(post)
                            .imageUrl(imgUrl)
                            .orderIndex(++maxOrder)
                            .build();
                    post.getImages().add(postImage);
                }
            } catch (IOException ioe) {
                log.error("Can not upload file!");
                ioe.printStackTrace();
            }

        }

        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost);
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
        return posts.stream().map(this::mapToResponse).collect(Collectors.toList());
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
        return posts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> searchPosts(String keyword, Long userId, Long lastPostId, int limit) {
        User user = getUserById(userId);
        Pageable pageable = PageRequest.of(0, limit);
        List<Post> posts = postRepository.searchFeedPosts(user.getId(), keyword, lastPostId, pageable);
        return posts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
}
