package com.social_media_be.repository;

import com.social_media_be.entity.Notification;
import com.social_media_be.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
        SELECT n FROM Notification n
        WHERE n.receiver.id = :receiverId
        ORDER BY COALESCE(n.updatedAt, n.createdAt) DESC
    """)
    Page<Notification> findByReceiverIdOrderByUpdatedAtCreatedAtDesc(@Param("receiverId") Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);
    
    java.util.Optional<Notification> findFirstByReceiverIdAndTypeAndReferenceIdAndIsReadFalse(Long receiverId, com.social_media_be.entity.enums.NotificationType type, Long referenceId);

    java.util.Optional<Notification> findFirstByTypeAndReferenceIdAndIsReadFalse(com.social_media_be.entity.enums.NotificationType type, Long referenceId);
    
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.type = :type AND n.actor.id = :actorId AND n.receiver.id = :receiverId")
    void deleteByTypeAndActorIdAndReceiverId(@Param("type") com.social_media_be.entity.enums.NotificationType type, 
                                            @Param("actorId") Long actorId, 
                                            @Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.id = :receiverId AND n.isRead = false")
    void markAllAsRead(@Param("receiverId") Long receiverId);

    Optional<Notification> findFirstByReceiverIdAndTypeAndReferenceId(Long receiverId, NotificationType type, Long referenceId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.referenceId = :referenceId AND n.type IN :types")
    void deleteByReferenceIdAndTypeIn(
            @Param("referenceId") Long referenceId,
            @Param("types") Collection<NotificationType> types
    );

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.referenceId IN :referenceIds AND n.type IN :types")
    void deleteByReferenceIdInAndTypeIn(
            @Param("referenceIds") Collection<Long> referenceIds,
            @Param("types") Collection<NotificationType> types
    );
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.targetId = :targetId")
    void deleteByTargetId(@Param("targetId") Long targetId);
}
