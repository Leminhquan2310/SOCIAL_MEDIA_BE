package com.social_media_be.repository;

import com.social_media_be.entity.Friendship;
import com.social_media_be.entity.enums.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.social_media_be.entity.User;
import java.util.Optional;

@Repository

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE " +
           "((f.requester.id = :userId1 AND f.receiver.id = :userId2) OR " +
           "(f.requester.id = :userId2 AND f.receiver.id = :userId1)) AND " +
           "f.status = :status")
    boolean existsByUsersAndStatus(@Param("userId1") Long userId1, @Param("userId2") Long userId2, @Param("status") FriendStatus status);

    Optional<Friendship> findByRequesterAndReceiver(User requester, User receiver);

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.requester.id = :userId1 AND f.receiver.id = :userId2) OR " +
           "(f.requester.id = :userId2 AND f.receiver.id = :userId1)")
    Optional<Friendship> findFriendshipBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT u FROM User u WHERE u.id IN (" +
           "SELECT f.receiver.id FROM Friendship f WHERE f.requester.id = :userId AND f.status = :status " +
           "UNION " +
           "SELECT f.requester.id FROM Friendship f WHERE f.receiver.id = :userId AND f.status = :status)")
    Page<User> findFriendsByUserId(@Param("userId") Long userId, @Param("status") FriendStatus status, Pageable pageable);

    @Query("SELECT f FROM Friendship f WHERE f.receiver.id = :receiverId AND f.status = :status")
    Page<Friendship> findPendingRequestsByReceiver(@Param("receiverId") Long receiverId, @Param("status") FriendStatus status, Pageable pageable);
    
    @Query("SELECT f FROM Friendship f WHERE f.requester.id = :requesterId AND f.status = :status")
    Page<Friendship> findPendingRequestsByRequester(@Param("requesterId") Long requesterId, @Param("status") FriendStatus status, Pageable pageable);

    @Query(value = "SELECT u.* FROM users u " +
           "JOIN friendships f1 ON (f1.requester_id = u.id OR f1.receiver_id = u.id) " +
           "JOIN friendships f2 ON (f2.requester_id = u.id OR f2.receiver_id = u.id) " +
           "WHERE (f1.requester_id = :userId1 OR f1.receiver_id = :userId1) AND f1.status = 'ACCEPTED' AND u.id != :userId1 " +
           "AND (f2.requester_id = :userId2 OR f2.receiver_id = :userId2) AND f2.status = 'ACCEPTED' AND u.id != :userId2",
           countQuery = "SELECT count(u.id) FROM users u " +
           "JOIN friendships f1 ON (f1.requester_id = u.id OR f1.receiver_id = u.id) " +
           "JOIN friendships f2 ON (f2.requester_id = u.id OR f2.receiver_id = u.id) " +
           "WHERE (f1.requester_id = :userId1 OR f1.receiver_id = :userId1) AND f1.status = 'ACCEPTED' AND u.id != :userId1 " +
           "AND (f2.requester_id = :userId2 OR f2.receiver_id = :userId2) AND f2.status = 'ACCEPTED' AND u.id != :userId2",
           nativeQuery = true)
    Page<User> findMutualFriends(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.id IN (" +
           "SELECT f.receiver.id FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'ACCEPTED' " +
           "UNION " +
           "SELECT f.requester.id FROM Friendship f WHERE f.receiver.id = :userId AND f.status = 'ACCEPTED')")
    long countFriends(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(u.id) FROM users u " +
            "JOIN friendships f1 ON (f1.requester_id = u.id OR f1.receiver_id = u.id) " +
            "JOIN friendships f2 ON (f2.requester_id = u.id OR f2.receiver_id = u.id) " +
            "WHERE (f1.requester_id = :userId1 OR f1.receiver_id = :userId1) AND f1.status = 'ACCEPTED' AND u.id != :userId1 " +
            "AND (f2.requester_id = :userId2 OR f2.receiver_id = :userId2) AND f2.status = 'ACCEPTED' AND u.id != :userId2",
            countQuery = "SELECT count(u.id) FROM users u " +
                    "JOIN friendships f1 ON (f1.requester_id = u.id OR f1.receiver_id = u.id) " +
                    "JOIN friendships f2 ON (f2.requester_id = u.id OR f2.receiver_id = u.id) " +
                    "WHERE (f1.requester_id = :userId1 OR f1.receiver_id = :userId1) AND f1.status = 'ACCEPTED' AND u.id != :userId1 " +
                    "AND (f2.requester_id = :userId2 OR f2.receiver_id = :userId2) AND f2.status = 'ACCEPTED' AND u.id != :userId2",
            nativeQuery = true)
    long countManualFriends(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
