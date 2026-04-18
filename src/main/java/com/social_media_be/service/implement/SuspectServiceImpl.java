package com.social_media_be.service.implement;

import com.social_media_be.dto.admin.AdminUserResponseDto;
import com.social_media_be.dto.admin.SuspectIpDto;
import com.social_media_be.entity.User;
import com.social_media_be.repository.IpBlacklistRepository;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.SuspectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuspectServiceImpl implements SuspectService {

    private final UserRepository userRepository;
    private final IpBlacklistRepository ipBlacklistRepository;

    @Override
    public List<SuspectIpDto> getSuspiciousIps(int threshold, int windowHours) {
        LocalDateTime since = LocalDateTime.now().minusHours(windowHours);
        List<Object[]> rows = userRepository.findSuspiciousIps(since, threshold);

        List<SuspectIpDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            String ip = (String) row[0];
            long count = ((Number) row[1]).longValue();

            // Fetch accounts for this IP to display in the Admin UI
            List<User> accounts = userRepository.findByRegistrationIp(ip);
            List<AdminUserResponseDto> accountDtos = accounts.stream()
                    .map(AdminUserResponseDto::fromEntity)
                    .toList();

            result.add(SuspectIpDto.builder()
                    .ip(ip)
                    .accountCount(count)
                    .blocked(ipBlacklistRepository.existsByIpAddress(ip))
                    .accounts(accountDtos)
                    .build());
        }

        log.info("Found {} suspicious IPs (threshold={}, window={}h)", result.size(), threshold, windowHours);
        return result;
    }

    @Override
    public List<AdminUserResponseDto> getUsersByIp(String ip) {
        return userRepository.findByRegistrationIp(ip).stream()
                .map(AdminUserResponseDto::fromEntity)
                .toList();
    }
}
