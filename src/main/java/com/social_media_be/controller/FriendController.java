package com.social_media_be.controller;

import com.social_media_be.dto.friend.FriendStatusDTO;
import com.social_media_be.dto.friend.FriendUserDTO;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.service.FriendService;
import com.social_media_be.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    // GET /api/friends?page=0&size=10
    @GetMapping("/{username}")
    public ResponseEntity<?> getFriends(
            @PathVariable(value = "username", required = false) String username,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if(username == null) username = userPrincipal.getUsername();
        Page<FriendUserDTO> friends = friendService.getFriends(
                userPrincipal,
                username,
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(friends));
    }

    // GET /api/friends/requests
    @GetMapping("/requests")
    public ResponseEntity<?> getFriendRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<FriendUserDTO> requests = friendService.getFriendRequests(
                userPrincipal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    // GET /api/friends/suggestions
    @GetMapping("/suggestions")
    public ResponseEntity<?> getSuggestions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<FriendUserDTO> suggestions = friendService.getSuggestions(
                userPrincipal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    // GET /api/friends/status/{userId}
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getRelationshipStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId) {
        FriendStatusDTO status = friendService.getRelationshipStatus(
                userPrincipal != null ? userPrincipal.getId() : null, userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    // GET /api/friends/mutual/{userId}
    @GetMapping("/mutual/{userId}")
    public ResponseEntity<?> getMutualFriends(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<FriendUserDTO> mutual = friendService.getMutualFriends(
                userPrincipal.getId(), userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(mutual));
    }

    // POST /api/friends/request/{userId}
    @PostMapping("/request/{userId}")
    public ResponseEntity<?> sendFriendRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId) {
        friendService.sendFriendRequest(userPrincipal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Friend request sent"));
    }

    // DELETE /api/friends/request/{userId}
    @DeleteMapping("/request/{userId}")
    public ResponseEntity<?> cancelFriendRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId) {
        friendService.cancelFriendRequest(userPrincipal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Friend request cancelled"));
    }

    // POST /api/friends/accept/{userId}
    @PostMapping("/accept/{userId}")
    public ResponseEntity<?> acceptFriendRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId) {
        friendService.acceptFriendRequest(userPrincipal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Friend request accepted"));
    }

    // POST /api/friends/decline/{userId}
    @PostMapping("/decline/{userId}")
    public ResponseEntity<?> declineFriendRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId) {
        friendService.declineFriendRequest(userPrincipal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Friend request declined"));
    }

    // DELETE /api/friends/{userId}
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> removeFriend(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId) {
        friendService.removeFriend(userPrincipal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Friend removed"));
    }
}
