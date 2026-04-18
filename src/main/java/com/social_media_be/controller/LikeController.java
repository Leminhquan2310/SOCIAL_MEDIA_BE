package com.social_media_be.controller;

import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.entity.enums.TargetType;
import com.social_media_be.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/toggle")
    public ResponseEntity<Void> toggleLike(
            @RequestParam Long targetId,
            @RequestParam TargetType targetType,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        likeService.toggleLike(targetId, targetType, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<Boolean> isLiked(
            @RequestParam Long targetId,
            @RequestParam TargetType targetType,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) return ResponseEntity.ok(false);
        return ResponseEntity.ok(likeService.isLiked(targetId, targetType, userPrincipal.getId()));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getLikeCount(
            @RequestParam Long targetId,
            @RequestParam TargetType targetType) {
        return ResponseEntity.ok(likeService.getLikeCount(targetId, targetType));
    }
}
