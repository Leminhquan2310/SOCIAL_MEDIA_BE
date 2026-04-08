package com.social_media_be.config.security.jwt;

import com.social_media_be.service.JWTService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.social_media_be.service.implement.CustomUserDetailsService;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JWTService jwtService;
  private final CustomUserDetailsService customUserDetailsService;
  private final com.social_media_be.repository.TokenBlacklistRepository tokenBlacklistRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      String jwt = extractJwtFromRequest(request);

      if (StringUtils.hasText(jwt)) {
        if (tokenBlacklistRepository.existsById(jwt)) {
          sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_BLACKLISTED",
              "Account already banned or token expired");
          return;
        }
        // ✅ Decode userId directly
        Long userId = jwtService.getUserIdFromToken(jwt);

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          UserDetails userDetails = customUserDetailsService.loadUserById(userId);
          if (jwtService.validateToken(jwt, userDetails)) {
            authenticateUser(userDetails, request);
          }
        }
      }
    } catch (ExpiredJwtException e) {
      log.warn("JWT token expired: {}", e.getMessage());
      sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED",
          "Token đã hết hạn, vui lòng đăng nhập lại");
      return; // ❗ return luôn, không gọi filterChain nữa
    } catch (Exception e) {
      log.error("Cannot set user authentication: {}", e.getMessage());
      sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_INVALID", "Token không hợp lệ");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void sendErrorResponse(HttpServletResponse response, int status, String errorCode, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(String.format(
        "{\"status\": %d, \"error\": \"%s\", \"message\": \"%s\"}",
        status, errorCode, message));
  }

  /**
   * Create authentication and set to SecurityContext
   */
  private void authenticateUser(UserDetails userDetails, HttpServletRequest request) {
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities());

    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    log.debug("User '{}' authenticated successfully", userDetails.getUsername());
  }

  /**
   * Extract JWT token from Authorization header
   */
  private String extractJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }

    return null;
  }
}
