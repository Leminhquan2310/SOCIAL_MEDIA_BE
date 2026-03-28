package com.social_media_be.repository;

import com.social_media_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import com.social_media_be.entity.enums.AuthProvider;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
    boolean existsByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
    
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query(value = "SELECT u.* FROM users u " +
           "WHERE u.id != :currentUserId " +
           "AND u.id NOT IN (" +
           "  SELECT f.requester_id FROM friendships f WHERE f.receiver_id = :currentUserId " +
           "  UNION " +
           "  SELECT f.receiver_id FROM friendships f WHERE f.requester_id = :currentUserId" +
           ") " +
           "ORDER BY RAND() ",
           countQuery = "SELECT COUNT(*) FROM users u " +
           "WHERE u.id != :currentUserId " +
           "AND u.id NOT IN (" +
           "  SELECT f.requester_id FROM friendships f WHERE f.receiver_id = :currentUserId " +
           "  UNION " +
           "  SELECT f.receiver_id FROM friendships f WHERE f.requester_id = :currentUserId" +
           ")",
           nativeQuery = true)
    Page<User> findFriendSuggestions(@Param("currentUserId") Long currentUserId, Pageable pageable);
}
