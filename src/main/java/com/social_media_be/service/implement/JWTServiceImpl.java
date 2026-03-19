package com.social_media_be.service.implement;

import com.social_media_be.entity.UserPrincipal;
import com.social_media_be.service.JWTService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Getter
public class JWTServiceImpl implements JWTService {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private long jwtExpirationMs;

  @Override
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();

    // Add roles to claims
    String roles = userDetails.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .collect(Collectors.joining(","));
    claims.put("roles", roles);
    // Add userId to claims to avoid using subject for multiple purposes
    if (userDetails instanceof UserPrincipal) {
      claims.put("userId", ((UserPrincipal) userDetails).getId());
    }

    return createToken(claims, userDetails.getUsername(), jwtExpirationMs);
  }

  @Override
  public String getUsernameFromToken(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  @Override
  public boolean validateToken(String token, UserDetails userDetails) {
    try {
      final String username = getUsernameFromToken(token);
      boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

      if (isValid) {
        log.debug("JWT token is valid for user: {}", username);
      } else {
        log.warn("JWT token validation failed for user: {}", username);
      }

      return isValid;
    } catch (SignatureException e) {
      log.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      log.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("JWT claims string is empty: {}", e.getMessage());
    }

    return false;
  }

  @Override
  public boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  @Override
  public long getExpirationTime(String token) {
    Date expiration = extractExpiration(token);
    return expiration.getTime() - System.currentTimeMillis();
  }

  public Long getUserIdFromToken(String token) {
    Claims claims = extractAllClaims(token);
    Object userIdClaim = claims.get("userId");
    if (userIdClaim == null) {
      // Fallback for older tokens if subject was ID (optional but recommended for safety)
      try {
        return Long.parseLong(claims.getSubject());
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return Long.valueOf(userIdClaim.toString());
  }
  // ========== Private Helper Methods ==========

  /**
   * Create JWT token with claims and subject
   */
  private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMs);

    return Jwts.builder()
      .setClaims(claims)
      .setSubject(subject)
      .setIssuedAt(now)
      .setExpiration(expiryDate)
      .signWith(getSigningKey(), SignatureAlgorithm.HS512)
      .compact();
  }

  /**
   * Extract specific claim from token
   */
  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /**
   * Extract all claims from token
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
      .setSigningKey(getSigningKey())
      .build()
      .parseClaimsJws(token)
      .getBody();
  }

  /**
   * Extract expiration date from token
   */
  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Get signing key from secret
   */
  private Key getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
