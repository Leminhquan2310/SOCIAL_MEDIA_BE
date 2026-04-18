package com.social_media_be.repository;

import com.social_media_be.entity.MessageMention;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageMentionRepository extends JpaRepository<MessageMention, Long> {
}
