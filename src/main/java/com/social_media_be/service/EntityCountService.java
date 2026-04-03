package com.social_media_be.service;

import com.social_media_be.entity.enums.TargetType;

public interface EntityCountService {
  void handlePostCommentCount(Long postId, boolean increment, int amount);

  void handleCommentReplyCount(Long commentId, boolean increment);
}
