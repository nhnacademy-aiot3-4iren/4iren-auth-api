package com.nhnacademy.userauthapi.dto;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest (
    @NotBlank
    String userId,
    @NotBlank
    String userPassword
){}
