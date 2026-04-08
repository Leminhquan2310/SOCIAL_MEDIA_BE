package com.social_media_be.service;

import com.social_media_be.dto.admin.SuspectIpDto;
import com.social_media_be.dto.admin.AdminUserResponseDto;
import java.util.List;

public interface SuspectService {
    /** IPs that registered >= threshold accounts within the last windowHours hours */
    List<SuspectIpDto> getSuspiciousIps(int threshold, int windowHours);

    /** Users belonging to a specific suspicious IP */
    List<AdminUserResponseDto> getUsersByIp(String ip);
}
