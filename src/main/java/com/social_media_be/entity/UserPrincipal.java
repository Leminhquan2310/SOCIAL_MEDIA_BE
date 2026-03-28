package com.social_media_be.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails, OAuth2User {
  private static final long serialVersionUID = 1L;
  private Long id;
  private String fullName;
  private String username;
  private String password;
  private String email;
  private boolean enabled;

  private Collection<? extends GrantedAuthority> roles;
  private Map<String, Object> attributes;

  public static UserPrincipal build(User user) {
    List<GrantedAuthority> authorities = user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
    return new UserPrincipal(
        user.getId(),
        user.getFullName(),
        user.getUsername(),
        user.getPassword(),
        user.getEmail(),
        user.isEnabled(),
        authorities,
        null);
  }

  public static UserPrincipal build(User user, Map<String, Object> attributes) {
    UserPrincipal userPrincipal = UserPrincipal.build(user);
    return new UserPrincipal(
        userPrincipal.getId(),
        userPrincipal.getFullName(),
        userPrincipal.getUsername(),
        userPrincipal.getPassword(),
        userPrincipal.getEmail(),
        userPrincipal.isEnabled(),
        userPrincipal.getAuthorities(),
        attributes);
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public String getName() {
    return fullName;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }
}
