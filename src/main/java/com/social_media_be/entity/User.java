package com.social_media_be.entity;

import com.social_media_be.entity.enums.AuthProvider;
import com.social_media_be.entity.enums.DisplayFriendsStatus;
import com.social_media_be.entity.enums.Gender;
import com.social_media_be.entity.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;


@Entity
@Table(name = "users",
  uniqueConstraints = {
    @UniqueConstraint(columnNames = {"auth_provider", "provider_id"})
  },
  indexes = {
    @Index(name = "IDX_USER_PROVIDER_ID", columnList = "auth_provider, provider_id")
  }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "password")
  private String password;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "user_role",
    joinColumns = {@JoinColumn(name = "user_id")},
    inverseJoinColumns = {@JoinColumn(name = "role_id")})
  private Set<Role> roles;

  @Column(name = "username", unique = true, length = 50)
  private String username;

  @Builder.Default
  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "full_name", length = 100)
  private String fullName;

  @Column(name = "email", length = 100)
  private String email;

  @Column(name = "address", columnDefinition = "TEXT")
  @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
  private String address;

  @Column(name = "phone", unique = true, length = 15)
  @Pattern(
    regexp = "^(\\+84|0)[0-9]{9,10}$",
    message = "Số điện thoại không hợp lệ"
  )
  private String phone;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", length = 10)
  private Gender gender;

  @Column(name = "hobby", length = 255)
  private String hobby;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private UserStatus status = UserStatus.PUBLIC;

  @Enumerated(EnumType.STRING)
  @Column(name = "display_friends_status", nullable = false)
  @Builder.Default
  private DisplayFriendsStatus displayFriendsStatus = DisplayFriendsStatus.PUBLIC;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_provider", nullable = false)
  private AuthProvider authProvider;

  @Column(name = "provider_id", nullable = false)
  private String providerId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;


  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (status == null) {
      status = UserStatus.PUBLIC;
    }
  }
}
