package com.social_media_be.repository;

import com.social_media_be.entity.Conversation;
import com.social_media_be.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c JOIN c.members m1 JOIN c.members m2 " +
           "WHERE c.type = 'PRIVATE' AND m1.user = :u1 AND m2.user = :u2")
    Optional<Conversation> findPrivateBetweenUsers(@Param("u1") User u1, @Param("u2") User u2);

    @Query("SELECT c FROM Conversation c JOIN c.members m WHERE m.user = :user ORDER BY c.lastMessageAt DESC")
    Page<Conversation> findAllByUser(@Param("user") User user, Pageable pageable);
}
