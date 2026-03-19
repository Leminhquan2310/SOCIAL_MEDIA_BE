package com.social_media_be.service;

import com.social_media_be.dto.auth.*;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refreshToken(String refreshToken);

    LogoutResponse logout(LogoutRequest logoutRequest);
}
