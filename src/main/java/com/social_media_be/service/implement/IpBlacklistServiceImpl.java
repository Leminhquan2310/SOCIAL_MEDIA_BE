package com.social_media_be.service.implement;

import com.social_media_be.entity.IpBlacklist;
import com.social_media_be.repository.IpBlacklistRepository;
import com.social_media_be.service.IpBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IpBlacklistServiceImpl implements IpBlacklistService {

    private final IpBlacklistRepository ipBlacklistRepository;

    @Override
    @Transactional
    public void blockIp(String ip, String reason) {
        if (!ipBlacklistRepository.existsByIpAddress(ip)) {
            IpBlacklist blacklist = IpBlacklist.builder()
                    .ipAddress(ip)
                    .reason(reason)
                    .build();
            ipBlacklistRepository.save(blacklist);
        }
    }

    @Override
    @Transactional
    public void unblockIp(String ip) {
        ipBlacklistRepository.findByIpAddress(ip).ifPresent(ipBlacklistRepository::delete);
    }

    @Override
    public boolean isIpBlacklisted(String ip) {
        return ipBlacklistRepository.existsByIpAddress(ip);
    }

    @Override
    public Page<IpBlacklist> getAllBlacklistedIps(Pageable pageable) {
        return ipBlacklistRepository.findAll(pageable);
    }
}
