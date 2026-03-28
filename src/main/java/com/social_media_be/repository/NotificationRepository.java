package com.social_media_be.repository;

import com.social_media_be.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);
    
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.type = :type AND n.actor.id = :actorId AND n.receiver.id = :receiverId")
    void deleteByTypeAndActorIdAndReceiverId(@Param("type") com.social_media_be.entity.enums.NotificationType type, 
                                            @Param("actorId") Long actorId, 
                                            @Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.id = :receiverId AND n.isRead = false")
    void markAllAsRead(@Param("receiverId") Long receiverId);
}
