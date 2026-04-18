package com.social_media_be.controller;

import com.social_media_be.dto.admin.AdminUserResponseDto;
import com.social_media_be.service.AdminUserService;
import com.social_media_be.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String keyword) {
        Sort sort = Sort.by(direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminUserResponseDto> users = adminUserService.getAllUsers(pageable, keyword);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        AdminUserResponseDto user = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long id, @RequestBody java.util.Map<String, String> request) {
        String reason = request.get("reason");
        adminUserService.banUser(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Banned account successfully"));
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Long id) {
        adminUserService.unbanUser(id);
        return ResponseEntity.ok(ApiResponse.success("Unbanned account successfully"));
    }
}
