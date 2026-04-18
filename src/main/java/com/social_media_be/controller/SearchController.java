package com.social_media_be.controller;

import com.social_media_be.dto.friend.FriendUserDTO;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.service.SearchService;
import com.social_media_be.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/users")
    public ResponseEntity<?> searchUsers(
            @RequestParam String q,
            @RequestParam(required = false) Integer lastExactMatch,
            @RequestParam(required = false) Integer lastMutualCount,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<FriendUserDTO> response = searchService.searchUsers(q, userPrincipal, lastExactMatch, lastMutualCount, lastId, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
