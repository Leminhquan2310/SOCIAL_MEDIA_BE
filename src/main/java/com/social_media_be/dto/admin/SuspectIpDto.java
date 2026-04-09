package com.social_media_be.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspectIpDto {
    private String ip;
    private long accountCount;
    private boolean blocked;
    private List<AdminUserResponseDto> accounts;
}
