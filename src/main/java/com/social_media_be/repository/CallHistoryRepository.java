package com.social_media_be.repository;

import com.social_media_be.entity.CallHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallHistoryRepository extends JpaRepository<CallHistory, Long> {
}
