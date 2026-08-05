package com.nhnacademy.userauthapi.dto.user;

import java.time.LocalDateTime;

// 회원 정보 응답 DTO
public record UserResponse(
        Long userId,
        String userLoginId,
        String userRole,
        String userEmail,
        String userName,
        String userStatus,
        LocalDateTime createdAt
) {}
