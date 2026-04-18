package com.social_media_be.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 50)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 32, message = "Password must be between 6 and 32 characters")
    private String password;

    @NotBlank(message = "Confirm Password is required")
    @Size(min = 6, max = 100)
    private String confirmPassword;

    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Pattern(
            regexp = "^$|^(0|\\+84)[0-9]{9}$",
            message = "Invalid phone number"
    )
    private String phone;

    private String role;
}
