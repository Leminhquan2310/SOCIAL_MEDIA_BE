package com.social_media_be.service.implement;

import com.social_media_be.entity.Comment;
import com.social_media_be.entity.Like;
import com.social_media_be.entity.Post;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.NotificationType;
import com.social_media_be.entity.enums.TargetType;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.repository.*;
import com.social_media_be.service.LikeService;
import com.social_media_be.service.NotificationService;
import com.social_media_be.service.EntityCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EntityCountService entityCountService;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void toggleLike(Long targetId, TargetType targetType, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        Optional<Like> existingLike = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            entityCountService.handleLikeCount(targetId, targetType, false);
//            notificationRepository.delete();
        } else {
            Like like = Like.builder()
                    .user(user)
                    .targetId(targetId)
                    .targetType(targetType)
                    .build();
            likeRepository.save(like);
            entityCountService.handleLikeCount(targetId, targetType, true);

            // Gửi thông báo
            sendLikeNotification(targetId, targetType, user);
        }
    }

    private void sendLikeNotification(Long targetId, TargetType targetType, User actor) {
        User receiver;
        NotificationType notificationType;
        Long referenceId;

        if (targetType == TargetType.POST) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với id: " + targetId));
            receiver = post.getUser();
            notificationType = NotificationType.LIKE_POST;
            referenceId = post.getId();
        } else {
            Comment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với id: " + targetId));
            receiver = comment.getUser();
            notificationType = NotificationType.LIKE_COMMENT;
            referenceId = comment.getId();
        }

        notificationService.createAndSendNotification(receiver, actor, notificationType, referenceId);
    }

    @Override
    public boolean isLiked(Long targetId, TargetType targetType, Long userId) {
        if (userId == null) return false;
        return likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
    }

    @Override
    public long getLikeCount(Long targetId, TargetType targetType) {
        return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
    }
}
