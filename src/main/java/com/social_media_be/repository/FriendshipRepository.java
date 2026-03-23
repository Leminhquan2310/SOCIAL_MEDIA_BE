package com.social_media_be.repository;

import com.social_media_be.entity.Friendship;
import com.social_media_be.entity.enums.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE " +
           "((f.requester.id = :userId1 AND f.receiver.id = :userId2) OR " +
           "(f.requester.id = :userId2 AND f.receiver.id = :userId1)) AND " +
           "f.status = :status")
    boolean existsByUsersAndStatus(@Param("userId1") Long userId1, @Param("userId2") Long userId2, @Param("status") FriendStatus status);
}
