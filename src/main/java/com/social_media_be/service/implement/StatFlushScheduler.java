package com.social_media_be.service.implement;

import com.social_media_be.entity.AppAccessStat;
import com.social_media_be.repository.AppAccessStatRepository;
import com.social_media_be.service.AccessCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Scheduler that periodically flushes the in-memory access counter to the database.
 * Runs every 60 seconds to avoid excessive DB writes on every single API request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatFlushScheduler {

    private final AccessCounterService accessCounterService;
    private final AppAccessStatRepository accessStatRepository;

    @Scheduled(fixedDelay = 60_000) // flush every 60 seconds
    @Transactional
    public void flushCounterToDatabase() {
        long count = accessCounterService.getAndReset();

        if (count == 0) return;

        LocalDate today = LocalDate.now();

        AppAccessStat stat = accessStatRepository.findByAccessDate(today)
                .orElseGet(() -> AppAccessStat.builder()
                        .accessDate(today)
                        .visitCount(0L)
                        .build());

        stat.setVisitCount(stat.getVisitCount() + count);
        accessStatRepository.save(stat);

        log.debug("Flushed {} visit(s) for date: {}", count, today);
    }
}
