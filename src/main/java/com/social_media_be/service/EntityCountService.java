package com.social_media_be.service;

import com.social_media_be.entity.Comment;
import com.social_media_be.entity.Post;
import com.social_media_be.entity.enums.TargetType;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.repository.CommentRepository;
import com.social_media_be.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntityCountService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void handleLikeCount(Long targetId, TargetType targetType, boolean increment) {
        if (targetType == TargetType.POST) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với id: " + targetId));
            int current = post.getLikeCount() != null ? post.getLikeCount() : 0;
            post.setLikeCount(increment ? current + 1 : Math.max(0, current - 1));
            postRepository.save(post);
        } else if (targetType == TargetType.COMMENT) {
            Comment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với id: " + targetId));
            int current = comment.getLikeCount() != null ? comment.getLikeCount() : 0;
            comment.setLikeCount(increment ? current + 1 : Math.max(0, current - 1));
            commentRepository.save(comment);
        }
    }

    @Transactional
    public void handlePostCommentCount(Long postId, boolean increment, int amount) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết với id: " + postId));
        int current = post.getCommentCount() != null ? post.getCommentCount() : 0;
        int delta = Math.max(0, amount);
        post.setCommentCount(increment ? current + delta : Math.max(0, current - delta));
        postRepository.save(post);
    }

    @Transactional
    public void handleCommentReplyCount(Long commentId, boolean increment) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với id: " + commentId));
        int current = comment.getReplyCount() != null ? comment.getReplyCount() : 0;
        comment.setReplyCount(increment ? current + 1 : Math.max(0, current - 1));
        commentRepository.save(comment);
    }
}
