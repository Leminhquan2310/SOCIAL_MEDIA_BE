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
import com.social_media_be.exception.UnauthorizedException;
import com.social_media_be.repository.*;
import com.social_media_be.service.CloudinaryService;
import com.social_media_be.service.CommentService;
import com.social_media_be.service.NotificationService;
import com.social_media_be.service.EntityCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final EntityCountService entityCountService;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto request, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với id: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        // Check Privacy
        validateCommentPermission(post, user);

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận cha với id: " + request.getParentCommentId()));
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new BadRequestException("Mã bình luận cha không thuộc bài viết này");
            }
        }
        Map<String, Object> uploadResult = null;
        if (request.getImage() != null) {
            try {
                uploadResult = (Map<String, Object>) cloudinaryService.upload(request.getImage(), "social-media/comments");
            } catch (IOException e) {
                e.printStackTrace();
                throw new BadRequestException("Upload image failed!");
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .imageUrl(uploadResult != null ? (String) uploadResult.get("secure_url") : null)
                .parentComment(parentComment)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // Tăng commentCount của Post
        entityCountService.handlePostCommentCount(postId, true, 1);
        
        // Nếu là reply, tăng replyCount của comment cha
        if (parentComment != null) {
            entityCountService.handleCommentReplyCount(parentComment.getId(), true);
        }

        // Send Notification
        if (parentComment != null) {
            // Reply notification
            if (!parentComment.getUser().getId().equals(userId)) {
                notificationService.createAndSendNotification(parentComment.getUser(), user, NotificationType.REPLY_COMMENT, savedComment.getId());
            }
        } else {
            // New comment notification to post owner
            if (!post.getUser().getId().equals(userId)) {
                notificationService.createAndSendNotification(post.getUser(), user, NotificationType.COMMENT_POST, post.getId());
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

        Map<String, Object> uploadResult = null;
        if (comment.getImageUrl() != null) {
            try {
                String publicId = cloudinaryService.extractPublicId(comment.getImageUrl());
                cloudinaryService.delete(publicId);
                uploadResult = (Map<String, Object>) cloudinaryService.upload(request.getImage(), "social-media/comments");
            } catch (IOException e) {
                e.printStackTrace();
                throw new BadRequestException("Upload image failed!");
            }
        }

        comment.setContent(request.getContent());
        comment.setImageUrl(uploadResult != null ? (String) uploadResult.get("secure_url") : null);
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
        int commentsToRemove = 1; // Bản thân comment này

        if (comment.getParentComment() == null) {
            // Nếu là comment gốc: lấy số lượng replies (vì sẽ bị xóa cascade)
            long replyCount = commentRepository.countByParentCommentId(commentId);
            commentsToRemove += (int) replyCount;
        } else {
            // Nếu là reply, giảm replyCount của comment cha
            entityCountService.handleCommentReplyCount(comment.getParentComment().getId(), false);
        }

        // Giảm commentCount của Post
        entityCountService.handlePostCommentCount(postId, false, commentsToRemove);

        commentRepository.delete(comment);
    }

    @Override
    public Page<CommentResponseDto> getCommentsByPost(Long postId, Long userId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtDesc(postId, pageable);
        return comments.map(c -> mapToResponseDto(c, userId));
    }

    @Override
    public List<CommentResponseDto> getReplies(Long parentCommentId, Long userId) {
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId);
        return replies.stream().map(c -> mapToResponseDto(c, userId)).collect(Collectors.toList());
    }

    private void validateCommentPermission(Post post, User user) {
        Long postOwnerId = post.getUser().getId();
        Long viewerId = user.getId();

        if (postOwnerId.equals(viewerId)) return; // Chủ bài viết luôn được comment

        Privacy privacy = post.getPrivacy();
        if (privacy == Privacy.ONLY_ME) {
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
                .authorAvatar(comment.getUser().getAvatarUrl())
                .content(comment.getContent())
                .imageUrl(comment.getImageUrl())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .parentCommentName(comment.getParentComment() != null ? comment.getParentComment().getUser().getFullName() : null)
                .likeCount(comment.getLikeCount())
                .isLiked(isLiked)
                .replyCount(comment.getReplyCount())
                .build();
    }
}
