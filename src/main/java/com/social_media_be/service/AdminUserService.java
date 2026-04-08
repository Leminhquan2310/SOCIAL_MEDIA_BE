package com.social_media_be.service;

import com.social_media_be.dto.admin.AdminUserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<AdminUserResponseDto> getAllUsers(Pageable pageable, String keyword);
    AdminUserResponseDto getUserById(Long id);
}
