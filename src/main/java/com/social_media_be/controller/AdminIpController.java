package com.social_media_be.controller;

import com.social_media_be.entity.IpBlacklist;
import com.social_media_be.service.IpBlacklistService;
import com.social_media_be.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ip-blacklist")
@RequiredArgsConstructor
public class AdminIpController {

    private final IpBlacklistService ipBlacklistService;

    @GetMapping
    public ResponseEntity<?> getAllBlacklistedIps(@PageableDefault(size = 10) Pageable pageable) {
        Page<IpBlacklist> blacklist = ipBlacklistService.getAllBlacklistedIps(pageable);
        return ResponseEntity.ok(ApiResponse.success(blacklist));
    }

    @PostMapping("/block")
    public ResponseEntity<?> blockIp(@RequestBody Map<String, String> request) {
        String ip = request.get("ip");
        String reason = request.getOrDefault("reason", "Violation of community standards");
        
        if (ip == null || ip.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400 , "Ip can not be empty"));
        }
        
        ipBlacklistService.blockIp(ip, reason);
        return ResponseEntity.ok(ApiResponse.success("Already take ip:  " + ip + " into blacklist"));
    }

    @DeleteMapping("/unblock/{ip}")
    public ResponseEntity<?> unblockIp(@PathVariable String ip) {
        ipBlacklistService.unblockIp(ip);
        return ResponseEntity.ok(ApiResponse.success("Unbanned for IP " + ip));
    }
}
