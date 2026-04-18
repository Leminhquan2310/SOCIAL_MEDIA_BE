package com.social_media_be.example;

import com.social_media_be.entity.Role;
import com.social_media_be.entity.User;
import com.social_media_be.entity.enums.AuthProvider;
import com.social_media_be.entity.enums.UserStatus;
import com.social_media_be.repository.RoleRepository;
import com.social_media_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  @Value("${app.avatar.default-url}")
  private String avatarDefault;

  @Override
  public void run(String... args) {

    // 1. Create roles if not exist
    Role roleUser = roleRepository.findByName("ROLE_USER")
      .orElseGet(() -> roleRepository.save(
        new Role(null, "ROLE_USER")
      ));

    Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
      .orElseGet(() -> roleRepository.save(
        new Role(null, "ROLE_ADMIN")
      ));

    // 2. Create admin user if not exist
    if (!userRepository.existsByAuthProviderAndProviderId(AuthProvider.LOCAL, "admin")) {
      User admin = User.builder()
        .providerId("admin")
        .username("admin")
        .authProvider(AuthProvider.LOCAL)
        .avatarUrl(avatarDefault)
        .email("admin@gmail.com")
        .password(passwordEncoder.encode("123456"))
        .roles(Set.of(roleAdmin))
        .enabled(true)
        .status(UserStatus.PUBLIC)
        .fullName("Admin")
        .build();

      userRepository.save(admin);
    }

    // 2. Create user test if not exist
    if (!userRepository.existsByAuthProviderAndProviderId(AuthProvider.LOCAL, "nguyenvana")) {
      User user = User.builder()
        .providerId("nguyenvana")
        .authProvider(AuthProvider.LOCAL)
        .username("nguyenvana")
        .email("nguyenvana@gmail.com")
        .avatarUrl(avatarDefault)
        .password(passwordEncoder.encode("123456"))
        .roles(Set.of(roleUser))
        .enabled(true)
        .status(UserStatus.PUBLIC)
        .fullName("Nguyen Van A")
        .build();

      userRepository.save(user);
    }
  }
}
