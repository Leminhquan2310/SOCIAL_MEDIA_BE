package com.social_media_be.repository;

import com.social_media_be.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostReportRepository extends JpaRepository<PostReport, Long> {
    boolean existsByReporterIdAndPostId(Long reporterId, Long postId);

    List<PostReport> findAllByPostId(Long postId);

    @Query("SELECT COUNT(DISTINCT pr.post.id) FROM PostReport pr")
    long countDistinctPostIds();
}
