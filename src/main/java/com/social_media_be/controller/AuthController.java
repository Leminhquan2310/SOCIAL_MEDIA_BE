package com.social_media_be.controller;

import com.social_media_be.dto.auth.*;
import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.service.AuthService;
import com.social_media_be.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j; // Thêm import

@Slf4j // Thêm annotation
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    Map<String, Object> userInfo = new HashMap<>();

    userInfo.put("id", userPrincipal.getId());
    userInfo.put("email", userPrincipal.getEmail());
    userInfo.put("name", userPrincipal.getName());

    return ResponseEntity.ok(ApiResponse.success(userInfo));
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<RegisterResponse>> register(
    @Valid @RequestBody RegisterRequest request) {
    RegisterResponse response = authService.register(request);
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(ApiResponse.created(response));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
    @Valid @RequestBody LoginRequest request,
    jakarta.servlet.http.HttpServletResponse response) {
    LoginResponse loginResponse = authService.login(request);

    // Thêm refresh token vào cookie cho Web clients
    addRefreshTokenCookie(response, loginResponse.getRefreshToken());

    return ResponseEntity.ok(ApiResponse.success(loginResponse));
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
    jakarta.servlet.http.HttpServletRequest request,
    jakarta.servlet.http.HttpServletResponse response,
    @RequestBody(required = false) RefreshTokenRequest tokenRequest) {

    // Ưu tiên lấy token từ cookie, nếu không có thì lấy từ body (cho mobile)
    String token = com.social_media_be.utils.CookieUtils.getCookie(request, "refreshToken")
      .map(jakarta.servlet.http.Cookie::getValue)
      .orElse(tokenRequest != null ? tokenRequest.getRefreshToken() : null);

    if (token == null || token.trim().isEmpty()) {
      throw new com.social_media_be.exception.BadRequestException("Refresh token is missing or empty");
    }

    RefreshTokenResponse tokenResponse = authService.refreshToken(token);
    
    // Cập nhật Refresh Token mới vào Cookie (Rotation)
    addRefreshTokenCookie(response, tokenResponse.getRefreshToken());
    
    return ResponseEntity.ok(ApiResponse.success(tokenResponse));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<LogoutResponse>> logout(
    jakarta.servlet.http.HttpServletRequest request,
    jakarta.servlet.http.HttpServletResponse response,
    @RequestBody(required = false) LogoutRequest logoutRequest) {

    String token = com.social_media_be.utils.CookieUtils.getCookie(request, "refreshToken")
      .map(jakarta.servlet.http.Cookie::getValue)
      .orElse(logoutRequest != null ? logoutRequest.getRefreshToken() : null);

    if (token != null && !token.trim().isEmpty()) {
      try {
        authService.logout(new LogoutRequest(token));
      } catch (Exception e) {
        // Ignore exception to ensure the cookie gets deleted on client side
        log.warn("Warning during logout (token may already be invalid/revoked): {}", e.getMessage());
      }
    }

    // Xóa cookie dù có token hay không để đảm bảo phía client sạch sẽ
    com.social_media_be.utils.CookieUtils.deleteCookie(request, response, "refreshToken");

    return ResponseEntity.ok(ApiResponse.success(LogoutResponse.builder().refreshToken(token).build()));
  }

  private void addRefreshTokenCookie(jakarta.servlet.http.HttpServletResponse response, String refreshToken) {
    // Thời gian sống của cookie (nên lấy từ config, ở đây dùng tạm 30 ngày tương đương RT)
    int maxAge = 30 * 24 * 60 * 60;
    com.social_media_be.utils.CookieUtils.addCookie(response, "refreshToken", refreshToken, maxAge);
  }
}
