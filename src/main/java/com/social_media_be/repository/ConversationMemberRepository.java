package com.social_media_be.repository;

import com.social_media_be.entity.Conversation;
import com.social_media_be.entity.ConversationMember;
import com.social_media_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
    
    Optional<ConversationMember> findByConversationAndUser(Conversation conversation, User user);
    
    List<ConversationMember> findByConversation(Conversation conversation);
    
    @Query("SELECT cm FROM ConversationMember cm WHERE cm.conversation = :conversation AND cm.user != :user")
    List<ConversationMember> findOtherMembers(@Param("conversation") Conversation conversation, @Param("user") User user);
    
    @Query("SELECT SUM(cm.unreadCount) FROM ConversationMember cm WHERE cm.user = :user")
    Long countTotalUnread(@Param("user") User user);
}
