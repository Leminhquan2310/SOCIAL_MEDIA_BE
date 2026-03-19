package com.social_media_be.service;

import org.springframework.security.core.userdetails.UserDetails;


public interface JWTService {

    String generateToken(UserDetails userDetails);

    String getUsernameFromToken(String token);

    boolean validateToken(String token, UserDetails userDetails);

    boolean isTokenExpired(String token);

    long getExpirationTime(String token);
}
