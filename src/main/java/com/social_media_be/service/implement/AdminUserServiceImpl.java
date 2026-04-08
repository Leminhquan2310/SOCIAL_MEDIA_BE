package com.social_media_be.service.implement;

import com.social_media_be.dto.admin.AdminUserResponseDto;
import com.social_media_be.entity.User;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final com.social_media_be.repository.RefreshTokenRepository refreshTokenRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> getAllUsers(Pageable pageable, String keyword) {
        Page<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userRepository
                    .findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            keyword.trim(), keyword.trim(), keyword.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(AdminUserResponseDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return AdminUserResponseDto.fromEntity(user);
    }

    @Override
    @Transactional
    public void banUser(Long id, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(false);
        user.setBannedAt(java.time.LocalDateTime.now());
        user.setBanReason(reason);
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);

        messagingTemplate.convertAndSend("/topic/user-status-" + id, "BANNED");
    }

    @Override
    @Transactional
    public void unbanUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(true);
        user.setBannedAt(null);
        user.setBanReason(null);
        userRepository.save(user);
    }
}
