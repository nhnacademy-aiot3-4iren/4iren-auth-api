package com.nhnacademy.userauthapi.dto;

import com.nhnacademy.userauthapi.entity.UserRole;
import com.nhnacademy.userauthapi.entity.UserStatus;
import lombok.Builder;

@Builder
public record UserLoginResponse (
        String userId,
        String userPassword,
        UserStatus userStatus,
        UserRole userRole
){}




