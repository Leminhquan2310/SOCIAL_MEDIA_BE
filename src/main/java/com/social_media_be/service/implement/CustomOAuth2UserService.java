package com.social_media_be.service.implement;

import com.social_media_be.config.security.oauth2.OAuth2UserInfo;
import com.social_media_be.config.security.oauth2.OAuth2UserInfoFactory;
import com.social_media_be.entity.Role;
import com.social_media_be.entity.User;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.enums.AuthProvider;
import com.social_media_be.exception.OAuth2AuthenticationProcessingException;
import com.social_media_be.exception.ResourceNotFoundException;
import com.social_media_be.repository.RoleRepository;
import com.social_media_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${app.avatar.default-url:https://example.com/default-avatar.png}")
    private String defaultAvatarUrl;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user: {}", ex.getMessage());
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,
                oAuth2User.getAttributes()
        );

        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // ✅ AUTO-LINK: Cập nhật provider nếu khác biệt (Trust provider)
            user.setProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
            user.setProviderId(oAuth2UserInfo.getId());
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(userRequest, oAuth2UserInfo);
        }

        return UserPrincipal.build(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest userRequest, OAuth2UserInfo oAuth2UserInfo) {
        log.info("Registering new OAuth2 user: {}", oAuth2UserInfo.getEmail());

        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: ROLE_USER"));

        Set<Role> roles = new HashSet<>();
        roles.add(roleUser);

        // ✅ GENERATE USERNAME TỪ EMAIL
        String username = generateUsername(oAuth2UserInfo.getEmail());

        User user = User.builder()
                .username(username)  // ✅ THÊM USERNAME
                .fullName(oAuth2UserInfo.getName())
                .email(oAuth2UserInfo.getEmail())
                .avatarUrl(StringUtils.hasText(oAuth2UserInfo.getImageUrl())
                        ? oAuth2UserInfo.getImageUrl()
                        : defaultAvatarUrl)  // ✅ FALLBACK TO DEFAULT
                .provider(AuthProvider.valueOf(
                        userRequest.getClientRegistration().getRegistrationId().toUpperCase()
                ))
                .providerId(oAuth2UserInfo.getId())
                .roles(roles)
                .emailVerified(true)
                .enabled(true)  // ✅ THÊM enabled = true
                .build();

        User savedUser = userRepository.save(user);
        log.info("New OAuth2 user registered: {} ({})", savedUser.getUsername(), savedUser.getProvider());

        return savedUser;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        boolean updated = false;

        if (StringUtils.hasText(oAuth2UserInfo.getName()) &&
                !oAuth2UserInfo.getName().equals(existingUser.getFullName())) {
            existingUser.setFullName(oAuth2UserInfo.getName());
            updated = true;
        }

        if (StringUtils.hasText(oAuth2UserInfo.getImageUrl()) &&
                !oAuth2UserInfo.getImageUrl().equals(existingUser.getAvatarUrl())) {
            existingUser.setAvatarUrl(oAuth2UserInfo.getImageUrl());
            updated = true;
        }

        if (updated) {
            log.info("Updated OAuth2 user info: {}", existingUser.getEmail());
            return userRepository.save(existingUser);
        }

        return existingUser;
    }

    /**
     * ✅ GENERATE UNIQUE USERNAME TỪ EMAIL
     */
    private String generateUsername(String email) {
        String baseUsername = email.split("@")[0].toLowerCase();

        // Remove special characters
        baseUsername = baseUsername.replaceAll("[^a-z0-9_]", "");

        // Ensure minimum length
        if (baseUsername.length() < 3) {
            baseUsername = "user_" + baseUsername;
        }

        String username = baseUsername;
        int counter = 1;

        // Find unique username
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
