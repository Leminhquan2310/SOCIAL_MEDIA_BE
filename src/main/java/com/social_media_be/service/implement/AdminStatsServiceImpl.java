package com.social_media_be.service.implement;

import com.social_media_be.dto.admin.AdminReportStatsDto;
import com.social_media_be.dto.admin.NewUserStatDto;
import com.social_media_be.dto.admin.VisitStatDto;
import com.social_media_be.entity.AppAccessStat;
import com.social_media_be.repository.AppAccessStatRepository;
import com.social_media_be.repository.PostReportRepository;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final AppAccessStatRepository accessStatRepository;
    private final UserRepository userRepository;
    private final PostReportRepository postReportRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VisitStatDto> getVisitStats(String range) {
        LocalDate today = LocalDate.now();
        LocalDate from = switch (range.toLowerCase()) {
            case "month" -> today.minusDays(29);
            case "year"  -> today.minusDays(364);
            default      -> today.minusDays(6); // week (7 days)
        };

        // Fetch from DB and map by date for O(1) lookup
        List<AppAccessStat> stats = accessStatRepository.findByDateRange(from, today);
        Map<LocalDate, Long> statMap = stats.stream()
                .collect(Collectors.toMap(AppAccessStat::getAccessDate, AppAccessStat::getVisitCount));

        // Build a full list — fill 0 for dates with no recorded traffic
        List<VisitStatDto> result = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(today)) {
            result.add(VisitStatDto.builder()
                    .date(cursor)
                    .visitCount(statMap.getOrDefault(cursor, 0L))
                    .build());
            cursor = cursor.plusDays(1);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReportStatsDto getReportStats() {
        long totalReportCount = postReportRepository.count();
        long totalReportedPosts = postReportRepository.countDistinctPostIds();
        return AdminReportStatsDto.builder()
                .totalReportCount(totalReportCount)
                .totalReportedPosts(totalReportedPosts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewUserStatDto> getNewUserStats(String range) {
        LocalDate today = LocalDate.now();
        LocalDate from = switch (range.toLowerCase()) {
            case "month" -> today.minusDays(29);
            case "year"  -> today.minusDays(364);
            default      -> today.minusDays(6); // week (7 days)
        };

        List<Object[]> rawStats = userRepository.countNewUsersByDateRange(from, today);
        Map<LocalDate, Long> statMap = rawStats.stream()
                .collect(Collectors.toMap(
                        row -> (LocalDate) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        List<NewUserStatDto> result = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(today)) {
            result.add(NewUserStatDto.builder()
                    .date(cursor)
                    .newUserCount(statMap.getOrDefault(cursor, 0L))
                    .build());
            cursor = cursor.plusDays(1);
        }

        return result;
    }
}
