package com.social_media_be.example;

import com.social_media_be.entity.Role;
import com.social_media_be.entity.User;
import com.social_media_be.enums.AuthProvider;
import com.social_media_be.enums.UserStatus;
import com.social_media_be.repository.RoleRepository;
import com.social_media_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    if (!userRepository.existsByUsername("admin")) {
      User admin = User.builder()
        .username("admin")
        .email("admin@gmail.com")
        .password(passwordEncoder.encode("123456"))
        .roles(Set.of(roleAdmin))
        .enabled(true)
        .status(UserStatus.PUBLIC)
        .provider(AuthProvider.LOCAL)
        .fullName("Admin")
        .build();

      userRepository.save(admin);
    }

    // 2. Create user test if not exist
    if (!userRepository.existsByUsername("nguyenvana")) {
      User user = User.builder()
        .username("nguyenvana")
        .email("nguyenvana@gmail.com")
        .password(passwordEncoder.encode("123456"))
        .roles(Set.of(roleUser))
        .provider(AuthProvider.LOCAL)
        .enabled(true)
        .status(UserStatus.PUBLIC)
        .fullName("Nguyen Van A")
        .build();

      userRepository.save(user);
    }
  }
}
