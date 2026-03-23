package com.social_media_be.repository;

import com.social_media_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.social_media_be.entity.enums.AuthProvider;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
    boolean existsByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
}
