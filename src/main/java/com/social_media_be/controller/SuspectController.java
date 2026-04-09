package com.social_media_be.controller;

import com.social_media_be.dto.admin.AdminUserResponseDto;
import com.social_media_be.dto.admin.SuspectIpDto;
import com.social_media_be.service.SuspectService;
import com.social_media_be.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/suspects")
@RequiredArgsConstructor
public class SuspectController {

    private final SuspectService suspectService;

    /**
     * GET /api/admin/suspects/multi-account-ips
     * Returns IPs with >= threshold accounts registered in the last windowHours.
     */
    @GetMapping("/multi-account-ips")
    public ResponseEntity<?> getSuspiciousIps(
            @RequestParam(defaultValue = "3") int threshold,
            @RequestParam(defaultValue = "24") int windowHours) {
        List<SuspectIpDto> suspects = suspectService.getSuspiciousIps(threshold, windowHours);
        return ResponseEntity.ok(ApiResponse.success(suspects));
    }

    /**
     * GET /api/admin/suspects/by-ip?ip=xxx
     * Returns all users registered from a given IP.
     */
    @GetMapping("/by-ip")
    public ResponseEntity<?> getUsersByIp(@RequestParam String ip) {
        List<AdminUserResponseDto> users = suspectService.getUsersByIp(ip);
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}
