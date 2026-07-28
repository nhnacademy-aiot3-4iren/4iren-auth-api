package com.nhnacademy.userauthapi.service.impl;

import com.nhnacademy.userauthapi.config.JwtProperties;
import com.nhnacademy.userauthapi.config.JwtProvider;
import com.nhnacademy.userauthapi.dto.LoginRequest;
import com.nhnacademy.userauthapi.dto.TokenResponse;
import com.nhnacademy.userauthapi.dto.UserLoginResponse;
import com.nhnacademy.userauthapi.entity.UserStatus;
import com.nhnacademy.userauthapi.exception.LoginFailException;
import com.nhnacademy.userauthapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.nhnacademy.userauthapi.client.UserClient;

import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor

public class AuthServiceImpl implements AuthService {

    private final UserClient userClient;
    private final PasswordEncoder encoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final JwtProperties jwtProperties;


    @Override
    public TokenResponse login(LoginRequest req) {
        //로그인 요청에서 아이디와 비번 추출
      String userId=req.userId();
      String password=req.userPassword();

        //아이디로 유저 조회
        UserLoginResponse resp=userClient.getUser(userId);

        //로그인 실패 조건: 유저가 존재하지 않거나, 비번이 일치하지 않거나, 계정이 활성화되어 있지 않은 경우
        if(resp==null
                || !encoder.matches(password,resp.userPassword())
                || !Objects.equals(resp.userStatus(),UserStatus.ACTIVE))
        {
            throw new LoginFailException("아이디 또는 비밀번호가 일치하지 않거나, 계정이 활성화 되어있지 않습니다.");
        }
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
        String userId= jwtProvider.getUserIdFromToken(accessTocken);

        //Redis에서 해당 유저의 리프레시 토큰 삭제
        String redisKey="refreshToken:" +userId;
        redisTemplate.delete(redisKey);

        //블랙리스트에 엑세스 토큰 저장(TTL:엑세스 토큰의 남은 유효기간)

        //남은 유효기간이 0보다 큰 경우에만 블랙리스트 등록(이미 만료된 토큰은 블랙리스트에 등록할 필요 없음)

    }


    //토큰 재발급
    @Override
    public TokenResponse refresh(String refreshToken) {
        return null;
    }
}
