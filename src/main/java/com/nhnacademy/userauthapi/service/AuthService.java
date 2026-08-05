package com.nhnacademy.userauthapi.service;

import com.nhnacademy.userauthapi.dto.token.TokenResponse;
import com.nhnacademy.userauthapi.dto.login.LoginRequest;

//인터페이스로 어떤 서비스 만들건지 선언만 해놈
public interface AuthService {
    TokenResponse login(LoginRequest req);

    void logout(String accessToken);

    TokenResponse refresh(String refreshToken);


}
