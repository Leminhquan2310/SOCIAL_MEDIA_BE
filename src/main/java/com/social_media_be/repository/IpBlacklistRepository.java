package com.social_media_be.repository;

import com.social_media_be.entity.IpBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Long> {
    Optional<IpBlacklist> findByIpAddress(String ipAddress);
    boolean existsByIpAddress(String ipAddress);
    void deleteByIpAddress(String ipAddress);
}
