package com.social_media_be.repository;

import com.social_media_be.entity.AppAccessStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppAccessStatRepository extends JpaRepository<AppAccessStat, Long> {

    Optional<AppAccessStat> findByAccessDate(LocalDate date);

    @Query("SELECT s FROM AppAccessStat s WHERE s.accessDate BETWEEN :from AND :to ORDER BY s.accessDate ASC")
    List<AppAccessStat> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
