package com.social_media_be.service;

import com.social_media_be.entity.IpBlacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IpBlacklistService {
    void blockIp(String ip, String reason);
    void unblockIp(String ip);
    boolean isIpBlacklisted(String ip);
    Page<IpBlacklist> getAllBlacklistedIps(Pageable pageable);
}
