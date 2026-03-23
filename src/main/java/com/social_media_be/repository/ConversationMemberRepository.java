package com.social_media_be.repository;

import com.social_media_be.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
}
