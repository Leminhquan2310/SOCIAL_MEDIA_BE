package com.social_media_be.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogoutRequest {
    @NotBlank
    @NotEmpty
    private String refreshToken;
}
