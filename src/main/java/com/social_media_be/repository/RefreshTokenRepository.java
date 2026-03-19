package com.social_media_be.repository;

import com.social_media_be.entity.RefreshToken;
import com.social_media_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    void deleteByUser(User user);

    Optional<RefreshToken> findByToken(String token);
}
