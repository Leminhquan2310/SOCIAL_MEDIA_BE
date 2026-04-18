package com.social_media_be.service.implement;

import com.social_media_be.buffer.LikeCommentCountBuffer;
import com.social_media_be.buffer.LikePostCountBuffer;
import com.social_media_be.repository.CommentRepository;
import com.social_media_be.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeFlushScheduler {

  private final LikePostCountBuffer postCountBuffer;
  private final LikeCommentCountBuffer commentCountBuffer;
  private final PostRepository postRepository;
  private final CommentRepository commentRepository;

  @Scheduled(fixedDelay = 5000) // chạy mỗi 5 giây
  @Transactional
  public void flush() {
    Map<Long, Long> pendingPost = postCountBuffer.drainAll();
    pendingPost.forEach((postId, delta) -> {
      postRepository.incrementLikeCount(postId, delta);
      log.info("Update like count for post {}: {}", postId, delta);
      // long newCount = postRepository.getLikeCount(postId);
      // milestoneService.checkAndNotify(postId, newCount);
    });

    Map<Long, Long> pendingComment = commentCountBuffer.drainAll();
    pendingComment.forEach((commentId, delta) -> {
      commentRepository.incrementLikeCount(commentId, delta);
    });
  }
}
