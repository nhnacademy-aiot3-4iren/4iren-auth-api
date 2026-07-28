package com.nhnacademy.userauthapi.service.impl;

import com.nhnacademy.userauthapi.client.AccountClient;
import com.nhnacademy.userauthapi.config.JwtProperties;
import com.nhnacademy.userauthapi.config.JwtProvider;
import com.nhnacademy.userauthapi.dto.TokenResponse;
import com.nhnacademy.userauthapi.dto.UserLoginResponse;
import com.nhnacademy.userauthapi.dto.login.LoginRequest;
import com.nhnacademy.userauthapi.dto.login.LoginResponse;
import com.nhnacademy.userauthapi.entity.UserStatus;
import com.nhnacademy.userauthapi.exception.LoginFailException;
import com.nhnacademy.userauthapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountClient accountClient;
    private final PasswordEncoder encoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final JwtProperties jwtProperties;


    @Override
    public TokenResponse login(LoginRequest req) {
        //로그인 요청에서 아이디와 비번 추출
      String userId=req.userLoginId();
      String password=req.userPassword();

      LoginResponse resp= accountClient.login(req).getBody();

        String role="ROLE_"+resp.userRole().toString();

        //로그인 성공 시, JWT 액세스 토큰과 리프레시 토큰 발급
        String accessToken= jwtProvider.createAccessToken(userId,role);
        String refreshToken=jwtProvider.createRefreshToken(userId);

        //발급된 리프레시 토큰을 Redis에 저장(키: "refreshToken:{userId}", 값: refreshToken)
        String redisKey="refreshToken:"+userId;
        redisTemplate.opsForValue().set(redisKey, refreshToken,jwtProperties.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);

        return new TokenResponse(accessToken, refreshToken);
    }

    //로그아웃
    @Override
    public void logout(String accessTocken) {
        //access토큰에서 유저 아이디 추출
//        String userId= jwtProvider.getUserIdFromToken(accessTocken);

        //Redis에서 해당 유저의 리프레시 토큰 삭제
//        String redisKey="refreshToken:" +userId;
//        redisTemplate.delete(redisKey);

        //블랙리스트에 엑세스 토큰 저장(TTL:엑세스 토큰의 남은 유효기간)

        //남은 유효기간이 0보다 큰 경우에만 블랙리스트 등록(이미 만료된 토큰은 블랙리스트에 등록할 필요 없음)

    }


    //토큰 재발급
    @Override
    public TokenResponse refresh(String refreshToken) {
        return null;
    }
}
