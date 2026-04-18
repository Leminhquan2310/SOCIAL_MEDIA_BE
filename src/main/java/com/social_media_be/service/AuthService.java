package com.social_media_be.service;

import com.social_media_be.dto.auth.*;

public interface AuthService {
    RegisterResponse register(RegisterRequest request, String registrationIp);

    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refreshToken(String refreshToken);

    void logout(LogoutRequest logoutRequest);
}
