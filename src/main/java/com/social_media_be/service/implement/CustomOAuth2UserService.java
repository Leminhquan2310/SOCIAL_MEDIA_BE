package com.social_media_be.service.implement;

import com.social_media_be.config.security.oauth2.OAuth2UserInfo;
import com.social_media_be.config.security.oauth2.OAuth2UserInfoFactory;
import com.social_media_be.entity.Role;
import com.social_media_be.entity.User;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.entity.enums.AuthProvider;
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

        AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());
        String providerId = oAuth2UserInfo.getId();
        Optional<User> userOptional = userRepository.findByAuthProviderAndProviderId(authProvider, providerId);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
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


        User user = User.builder()
                .authProvider(AuthProvider.valueOf(
                        userRequest.getClientRegistration().getRegistrationId().toUpperCase()
                ))
                .providerId(oAuth2UserInfo.getId())
                .username(generateUniqueUsername(oAuth2UserInfo.getName()))
                .fullName(oAuth2UserInfo.getName())
                .email(oAuth2UserInfo.getEmail())
                .avatarUrl(StringUtils.hasText(oAuth2UserInfo.getImageUrl())
                        ? oAuth2UserInfo.getImageUrl()
                         : defaultAvatarUrl)
                .roles(roles)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New OAuth2 user registered: {} ({}) with username: {}", savedUser.getProviderId(), savedUser.getAuthProvider(), savedUser.getUsername());

        return savedUser;
    }

    private String generateUniqueUsername(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            fullName = "user";
        }
        
        // Convert to lowercase, remove non-alphanumeric, and remove Vietnamese accents
        String base = fullName.toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[^a-z0-9]", "");
        
        if (base.isEmpty()) base = "user";

        String username = base;
        int maxAttempts = 10;
        int attempts = 0;
        
        while (userRepository.existsByUsername(username) && attempts < maxAttempts) {
            String suffix = UUID.randomUUID().toString().substring(0, 4);
            username = base + "_" + suffix;
            attempts++;
        }
        
        return username;
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

}
