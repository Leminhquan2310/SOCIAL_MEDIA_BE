package com.social_media_be.dto.user;

import com.social_media_be.entity.enums.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    @Size(max = 100, message = "Tên không quá 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 255, message = "Sở thích không quá 255 ký tự")
    private String hobby;

    @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
    private String address;
}
