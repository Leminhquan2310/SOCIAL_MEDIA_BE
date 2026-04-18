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
import java.util.List;

import com.social_media_be.dto.user.UserSearchProjection;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
    boolean existsByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
    
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    Page<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String fullName, String email, Pageable pageable);

    @Query(value = "SELECT u.* FROM users u " +
           "WHERE u.id != :currentUserId " +
           "AND u.id NOT IN (" +
           "  SELECT f.requester_id FROM friendships f WHERE f.receiver_id = :currentUserId " +
           "  UNION " +
           "  SELECT f.receiver_id FROM friendships f WHERE f.requester_id = :currentUserId" +
           ") " +
           "AND u.id NOT IN (SELECT ur.user_id FROM user_role ur JOIN roles r ON ur.role_id = r.id WHERE r.name = 'ROLE_ADMIN') " +
           "ORDER BY RAND() ",
           countQuery = "SELECT COUNT(*) FROM users u " +
           "WHERE u.id != :currentUserId " +
           "AND u.id NOT IN (" +
           "  SELECT f.requester_id FROM friendships f WHERE f.receiver_id = :currentUserId " +
           "  UNION " +
           "  SELECT f.receiver_id FROM friendships f WHERE f.requester_id = :currentUserId" +
           ") " +
           "AND u.id NOT IN (SELECT ur.user_id FROM user_role ur JOIN roles r ON ur.role_id = r.id WHERE r.name = 'ROLE_ADMIN')",
           nativeQuery = true)
    Page<User> findFriendSuggestions(@Param("currentUserId") Long currentUserId, Pageable pageable);

    @Query(value = "SELECT * FROM (" +
           "  SELECT u.id, u.username, u.full_name as fullName, u.avatar_url as avatarUrl, " +
           "  (CASE WHEN LOWER(u.username) = LOWER(:query) OR LOWER(u.full_name) = LOWER(:query) THEN 1 ELSE 0 END) as exact_match, " +
           "  (SELECT COUNT(*) FROM (" +
           "      SELECT f1.friend_id FROM (" +
           "          SELECT receiver_id as friend_id FROM friendships WHERE requester_id = :currentUserId AND status = 'ACCEPTED' " +
           "          UNION SELECT requester_id as friend_id FROM friendships WHERE receiver_id = :currentUserId AND status = 'ACCEPTED' " +
           "      ) f1 " +
           "      JOIN (" +
           "          SELECT receiver_id as friend_id FROM friendships WHERE requester_id = u.id AND status = 'ACCEPTED' " +
           "          UNION SELECT requester_id as friend_id FROM friendships WHERE receiver_id = u.id AND status = 'ACCEPTED' " +
           "      ) f2 ON f1.friend_id = f2.friend_id " +
           "  ) as m) as mutual_count " +
           "  FROM users u " +
           "  WHERE u.id != :currentUserId " +
           "  AND u.id NOT IN (SELECT ur.user_id FROM user_role ur JOIN roles r ON ur.role_id = r.id WHERE r.name = 'ROLE_ADMIN') " +
           "  AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           ") as result " +
           "WHERE (:lastExactMatch IS NULL OR " +
           "      (exact_match < :lastExactMatch) OR " +
           "      (exact_match = :lastExactMatch AND mutual_count < :lastMutualCount) OR " +
           "      (exact_match = :lastExactMatch AND mutual_count = :lastMutualCount AND id < :lastId)) " +
           "ORDER BY exact_match DESC, mutual_count DESC, id DESC " +
           "LIMIT :limit",
           nativeQuery = true)
    List<UserSearchProjection> searchUsers(
            @Param("query") String query,
            @Param("currentUserId") Long currentUserId,
            @Param("lastExactMatch") Integer lastExactMatch,
            @Param("lastMutualCount") Integer lastMutualCount,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

    @Query("SELECT CAST(u.createdAt AS LocalDate) as date, COUNT(u.id) as count " +
           "FROM User u " +
           "WHERE CAST(u.createdAt AS LocalDate) BETWEEN :from AND :to " +
           "AND u.id NOT IN (SELECT ur.id FROM User ur JOIN ur.roles r WHERE r.name = 'ROLE_ADMIN') " +
           "GROUP BY CAST(u.createdAt AS LocalDate) " +
           "ORDER BY CAST(u.createdAt AS LocalDate) ASC")
    List<Object[]> countNewUsersByDateRange(@Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to);

    // Phát hiện spam: IP có từ :threshold account trở lên trong :hours giờ gần nhất
    @Query(value = "SELECT u.registration_ip AS ip, COUNT(*) AS accountCount " +
           "FROM users u " +
           "WHERE u.registration_ip IS NOT NULL " +
           "AND u.created_at >= :since " +
           "GROUP BY u.registration_ip " +
           "HAVING COUNT(*) >= :threshold " +
           "ORDER BY accountCount DESC",
           nativeQuery = true)
    List<Object[]> findSuspiciousIps(
            @Param("since") java.time.LocalDateTime since,
            @Param("threshold") int threshold);

    // Lấy danh sách user theo IP đăng ký
    @Query("SELECT u FROM User u WHERE u.registrationIp = :ip ORDER BY u.createdAt ASC")
    List<User> findByRegistrationIp(@Param("ip") String ip);
}
