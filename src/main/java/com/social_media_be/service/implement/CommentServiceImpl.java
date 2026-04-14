package com.social_media_be.service.implement;

import com.social_media_be.dto.comment.CommentRequestDto;
import com.social_media_be.dto.comment.CommentResponseDto;
import com.social_media_be.entity.Comment;
import com.social_media_be.entity.Post;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.FriendStatus;
import com.social_media_be.entity.enums.NotificationType;
import com.social_media_be.entity.enums.Privacy;
import com.social_media_be.entity.enums.TargetType;
import com.social_media_be.exception.BadRequestException;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.exception.TooManyRequestsException;
import com.social_media_be.exception.UnauthorizedException;
import com.social_media_be.repository.*;
import com.social_media_be.service.CloudinaryService;
import com.social_media_be.service.CommentRateLimiter;
import com.social_media_be.service.CommentService;
import com.social_media_be.service.ContentModerationService;
import com.social_media_be.entity.enums.*;
import com.social_media_be.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final EntityCountServiceImpl entityCountService;
    private final CloudinaryService cloudinaryService;
    private final NotificationRepository notificationRepository;
    private final CommentRateLimiter commentRateLimiter;
    private final ContentModerationService contentModerationService;

    @Override
    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto request, Long userId) {
        // Rate limit check: prevent comment spam
        if (!commentRateLimiter.isAllowed(userId)) {
            throw new TooManyRequestsException("Bạn đang bình luận quá nhanh. Vui lòng chờ một chút trước khi tiếp tục.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với id: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        // Check Privacy
        validateCommentPermission(post, user);
        contentModerationService.validateContent(request.getContent(), "Comment contains restricted words");

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận cha với id: " + request.getParentCommentId()));
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new BadRequestException("Mã bình luận cha không thuộc bài viết này");
            }
        }
        String mediaUrl = null;
        MediaType mediaType = MediaType.IMAGE;
        double violationScore = 0.0;
        MediaStatus status = MediaStatus.ACTIVE;

        MultipartFile mediaFile = request.getVideo() != null ? request.getVideo() : request.getImage();
        if (mediaFile != null) {
            try {
                MediaType type = request.getVideo() != null ? MediaType.VIDEO : MediaType.IMAGE;
                String resourceType = type == MediaType.VIDEO ? "video" : "image";
                
                // Validate size for video
                if (type == MediaType.VIDEO && mediaFile.getSize() > 100 * 1024 * 1024) {
                    throw new BadRequestException("Video size exceeds 100MB limit.");
                }

                Map<String, Object> uploadResult = (Map<String, Object>) cloudinaryService.upload(mediaFile, "social-media/comments", resourceType, true);
                mediaUrl = (String) uploadResult.get("secure_url");
                mediaType = type;
                violationScore = extractViolationScore(uploadResult);
                
                if (violationScore >= 0.8) {
                    status = MediaStatus.REJECTED;
                } else if (violationScore >= 0.5) {
                    status = MediaStatus.FLAGGED;
                }
            } catch (IOException e) {
                log.error("Upload media failed!", e);
                throw new BadRequestException("Upload media failed!");
            }
        }

        // LOGIC 2-LEVEL: Nếu parentComment đang là một reply (đã có parent), 
        // thì đẩy reply mới này lên cùng hàng với parentComment đó (cùng trỏ về Root).
        Comment actualParent = parentComment;
        User replyToUser = null;
        if (parentComment != null) {
            replyToUser = parentComment.getUser(); // Người mà user thực sự nhấn Reply
            if (parentComment.getParentComment() != null) {
                actualParent = parentComment.getParentComment();
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .violationScore(violationScore)
                .status(status)
                .parentComment(actualParent)
                .replyToUser(replyToUser)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // Tăng commentCount của Post
        entityCountService.handlePostCommentCount(postId, true, 1);

        // Nếu là reply, tăng replyCount của comment cha (luôn là Root)
        if (actualParent != null) {
            entityCountService.handleCommentReplyCount(actualParent.getId(), true);
        }

        // Send Notification
        if (parentComment != null) {
            // Reply notification
            if (!parentComment.getUser().getId().equals(userId)) {
                notificationService.createAndSendNotification(parentComment.getUser(), user, NotificationType.REPLY_COMMENT, savedComment.getId(), post.getId());
            }
        } else {
            // New comment notification to post owner
            if (!post.getUser().getId().equals(userId)) {
                notificationService.createAndSendNotification(post.getUser(), user, NotificationType.COMMENT_POST, savedComment.getId(), post.getId());
            }
        }

        return mapToResponseDto(savedComment, userId);
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto request, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với id: " + commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền chỉnh sửa bình luận này");
        }

        comment.setContent(request.getContent());
        comment.setEdited(true);
        Comment updatedComment = commentRepository.save(comment);

        return mapToResponseDto(updatedComment, userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với id: " + commentId));

        // Chủ nhân bình luận hoặc chủ nhân bài viết có thể xóa
        if (!comment.getUser().getId().equals(userId) && !comment.getPost().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xóa bình luận này");
        }

        Long postId = comment.getPost().getId();
        int commentsToRemove = 1;

        // 1. Phân loại và thu thập IDs cần xóa (Likes, Notifications)
        List<Long> targetIdsToDelete = new java.util.ArrayList<>();
        targetIdsToDelete.add(commentId);

        List<String> imageUrlsToDelete = new java.util.ArrayList<>();
        if (comment.getMediaUrl() != null) imageUrlsToDelete.add(comment.getMediaUrl());

        if (comment.getParentComment() == null) {
            // Nếu là comment gốc: lấy danh sách replies để dọn dẹp
            List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
            for (Comment reply : replies) {
                targetIdsToDelete.add(reply.getId());
                if (reply.getMediaUrl() != null) imageUrlsToDelete.add(reply.getMediaUrl());
            }
            commentsToRemove += replies.size();
        } else {
            // Nếu là reply, giảm replyCount của comment cha (Root)
            entityCountService.handleCommentReplyCount(comment.getParentComment().getId(), false);
        }

        // 2. Dọn dẹp Cloudinary
        for (String url : imageUrlsToDelete) {
            try {
                String publicId = cloudinaryService.extractPublicId(url);
                if (publicId != null) {
                    cloudinaryService.delete(publicId);
                }
            } catch (Exception e) {
                log.error("Failed to delete media from Cloudinary: {}", url, e);
            }
        }

        // 3. Dọn dẹp Database (Likes & Notifications)
        likeRepository.deleteByTargetIdInAndTargetType(targetIdsToDelete, TargetType.COMMENT);
        
        // Notifications: Xóa thông báo liên quan đến comment hoặc reply (REPLY_COMMENT, LIKE_COMMENT)
        notificationRepository.deleteByReferenceIdInAndTypeIn(
            targetIdsToDelete, 
            java.util.Arrays.asList(NotificationType.REPLY_COMMENT, NotificationType.LIKE_COMMENT)
        );

        // 4. Cập nhật số lượng Post
        entityCountService.handlePostCommentCount(postId, false, commentsToRemove);

        // 5. Xóa thực thể (Cascade sẽ xóa nốt các replies trong DB nếu là Root)
        commentRepository.delete(comment);
    }

    @Override
    public Page<CommentResponseDto> getCommentsByPost(Long postId, Long userId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bai viet voi id: " + postId));
        validateCommentVisibility(post, userId);
        Page<Comment> comments = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtDesc(postId, pageable);
        return comments.map(c -> mapToResponseDto(c, userId));
    }

    @Override
    public List<CommentResponseDto> getReplies(Long parentCommentId, Long userId) {
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay binh luan voi id: " + parentCommentId));
        validateCommentVisibility(parentComment.getPost(), userId);
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId);
        return replies.stream().map(c -> mapToResponseDto(c, userId)).collect(Collectors.toList());
    }

    private void validateCommentVisibility(Post post, Long userId) {
        Long postOwnerId = post.getUser().getId();

        if (post.getPrivacy() == Privacy.HIDDEN) {
            throw new ResourceNotFoundException("Khong tim thay bai viet voi id: " + post.getId());
        }

        if (userId != null && postOwnerId.equals(userId)) {
            return;
        }

        if (post.getPrivacy() == Privacy.PUBLIC) {
            return;
        }

        if (post.getPrivacy() == Privacy.ONLY_ME) {
            throw new ResourceNotFoundException("Khong tim thay bai viet voi id: " + post.getId());
        }

        if (post.getPrivacy() == Privacy.FRIEND_ONLY) {
            boolean isFriend = userId != null
                    && friendshipRepository.existsByUsersAndStatus(postOwnerId, userId, FriendStatus.ACCEPTED);
            if (!isFriend) {
                throw new ResourceNotFoundException("Khong tim thay bai viet voi id: " + post.getId());
            }
        }
    }

    private void validateCommentPermission(Post post, User user) {
        Long postOwnerId = post.getUser().getId();
        Long viewerId = user.getId();

        if (postOwnerId.equals(viewerId)) return; // Chủ bài viết luôn được comment

        Privacy privacy = post.getPrivacy();
        if (privacy == Privacy.ONLY_ME || privacy == Privacy.HIDDEN) {
            throw new BadRequestException("Bài viết này không cho phép người khác bình luận");
        }

        if (privacy == Privacy.FRIEND_ONLY) {
            boolean isFriend = friendshipRepository.existsByUsersAndStatus(postOwnerId, viewerId, FriendStatus.ACCEPTED);
            if (!isFriend) {
                throw new BadRequestException("Chỉ bạn bè mới có thể bình luận bài viết này");
            }
        }
        // PUBLIC is allowed for all logged in users
    }

    private CommentResponseDto mapToResponseDto(Comment comment, Long userId) {
        boolean isLiked = false;
        if (userId != null) {
            isLiked = likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, comment.getId(), TargetType.COMMENT);
        }

        return CommentResponseDto.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getUser().getId())
                .authorName(comment.getUser().getFullName() != null ? comment.getUser().getFullName() : comment.getUser().getUsername())
                .authorUsername(comment.getUser().getUsername())
                .authorAvatar(comment.getUser().getAvatarUrl())
                .content(comment.getContent())
                .mediaUrl(comment.getStatus() == MediaStatus.ACTIVE ? comment.getMediaUrl() : null) // Hide non-active media
                .mediaType(comment.getMediaType())
                .mediaStatus(comment.getStatus()) // Added status to DTO for admin/frontend awareness
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .parentCommentId(comment.getReplyToUser() != null ? comment.getReplyToUser().getId() : null)
                .parentCommentName(comment.getReplyToUser() != null ? comment.getReplyToUser().getFullName() : null)
                .parentCommentUsername(comment.getReplyToUser() != null ? comment.getReplyToUser().getUsername() : null)
                .likeCount(comment.getLikeCount())
                .isLiked(isLiked)
                .replyCount(comment.getReplyCount())
                .edited(comment.isEdited())
                .build();
    }

    private double extractViolationScore(Map<String, Object> uploadResult) {
        try {
            List<Map<String, Object>> moderation = (List<Map<String, Object>>) uploadResult.get("moderation");
            if (moderation == null || moderation.isEmpty()) return 0.0;

            for (Map<String, Object> mod : moderation) {
                if ("aws_rek".equals(mod.get("kind")) || "google_vision".equals(mod.get("kind"))) {
                    Map<String, Object> response = (Map<String, Object>) mod.get("response");
                    if (response == null) continue;

                    List<Map<String, Object>> labels = (List<Map<String, Object>>) response.get("ModerationLabels");
                    if (labels == null) {
                        labels = (List<Map<String, Object>>) response.get("moderation_labels");
                    }
                    if (labels == null) {
                        labels = (List<Map<String, Object>>) response.get("labels");
                    }

                    if (labels != null) {
                        return labels.stream()
                                .mapToDouble(l -> {
                                    Object confidence = l.get("Confidence");
                                    if (confidence == null) confidence = l.get("confidence");
                                    if (confidence instanceof Number) return ((Number) confidence).doubleValue() / 100.0;
                                    return 0.0;
                                })
                                .max().orElse(0.0);
                    }
                }
            }
        } catch (Exception e) {
            // log warn via syserr or similar if log not available
        }
        return 0.0;
    }
}
