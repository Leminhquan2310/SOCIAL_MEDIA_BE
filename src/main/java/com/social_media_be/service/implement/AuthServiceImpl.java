package com.social_media_be.service.implement;

import com.social_media_be.dto.auth.*;
import com.social_media_be.entity.RefreshToken;
import com.social_media_be.entity.Role;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.AuthProvider;
import com.social_media_be.exception.BadRequestException;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.repository.RefreshTokenRepository;
import com.social_media_be.repository.RoleRepository;
import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.AuthService;
import com.social_media_be.service.JWTService;
import com.social_media_be.service.RefreshTokenService;  // ✅ THÊM
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.avatar.default-url}")
    private String defaultAvatarUrl;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenDurationMs;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request, String registrationIp) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        User user = new User();
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setProviderId(request.getUsername());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setAvatarUrl(defaultAvatarUrl);
        user.setEnabled(true);
        user.setRegistrationIp(registrationIp);

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role USER not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getProviderId());

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getProviderId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .message("User registered successfully")
                .build();
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByAuthProviderAndProviderId(AuthProvider.LOCAL, userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshTokenObj = refreshTokenService.createRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenObj.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime(accessToken))
                .build();
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");

        RefreshToken stored = refreshTokenService.findByToken(refreshToken);

        if (stored.isRevoked() || stored.getExpiryDate().isBefore(java.time.Instant.now())) {
            // Nếu RT đã bị hủy hoặc hết hạn, ném lỗi
            throw new BadRequestException("Refresh token expired or revoked");
        }

        // --- BẮT ĐẦU REFRESH TOKEN ROTATION ---
        User user = stored.getUser();
        
        // 1. Xóa (hoặc hủy) token cũ ngay lập tức sau khi dùng
        refreshTokenRepository.delete(stored);
        
        // 2. Tạo Refresh Token mới cho lần kế tiếp
        RefreshToken newRefreshTokenObj = refreshTokenService.createRefreshToken(user.getId());
        
        // 3. Tạo Access Token mới
        UserDetails userDetails = ((com.social_media_be.service.implement.CustomUserDetailsService) userDetailsService).loadUserById(user.getId());
        String newAccessToken = jwtService.generateToken(userDetails);

        log.info("Token rotated successfully for user: {}", user.getProviderId());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenObj.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime(newAccessToken))
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest logoutRequest) {
        RefreshToken stored = refreshTokenService.findByToken(logoutRequest.getRefreshToken());
        refreshTokenRepository.delete(stored);
    }
}
