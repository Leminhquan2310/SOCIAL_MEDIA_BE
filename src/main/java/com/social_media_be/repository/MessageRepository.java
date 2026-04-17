package com.social_media_be.repository;

import com.social_media_be.entity.Conversation;
import com.social_media_be.entity.Message;
import com.social_media_be.entity.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findAllByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.status = :status, m.seenAt = :now WHERE m.conversation = :conv AND m.sender.id != :userId AND m.status != :status")
    void markAllAsSeen(@Param("conv") Conversation conv, 
                       @Param("userId") Long userId, 
                       @Param("status") MessageStatus status,
                       @Param("now") LocalDateTime now);
}
