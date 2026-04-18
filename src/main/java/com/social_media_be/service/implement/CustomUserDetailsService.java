package com.social_media_be.service.implement;

import com.social_media_be.entity.User;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.entity.enums.AuthProvider;
import org.springframework.security.authentication.DisabledException;
import com.social_media_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByAuthProviderAndProviderId(AuthProvider.LOCAL, username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with identify: " + username));

        // Delegating account lock/disable checks to Spring Security's UserPrincipal
        // object
        return UserPrincipal.build(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with id: " + id));

        if (!user.isEnabled()) {
            throw new DisabledException("User account is banned or removed");
        }

        return UserPrincipal.build(user);
    }
}
