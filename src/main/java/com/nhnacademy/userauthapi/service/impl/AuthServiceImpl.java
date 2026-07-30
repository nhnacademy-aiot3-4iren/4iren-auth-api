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
import com.nhnacademy.userauthapi.exception.RefreshTokenValidateException;
import com.nhnacademy.userauthapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountClient accountClient;
    private final PasswordEncoder encoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;
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
    public void logout(String accessToken) {
        //access토큰에서 유저 아이디 추출
        String userId= jwtProvider.getUserIdFromToken(accessToken);

        //Redis에서 해당 유저의 리프레시 토큰 삭제
        String redisKey="refreshToken:" +userId;
        redisTemplate.delete(redisKey);

        //블랙리스트에 엑세스 토큰 저장(TTL:엑세스 토큰의 남은 유효기간)
        long remainingMillsSeconds=jwtProvider.getRemainingTime(accessToken);

        //남은 유효기간이 0보다 큰 경우에만 블랙리스트 등록(이미 만료된 토큰은 블랙리스트에 등록할 필요 없음)
        if(remainingMillsSeconds>0){
            String key="blacklist:"+accessToken;
            redisTemplate.opsForValue().set(key,"logout",remainingMillsSeconds,TimeUnit.MILLISECONDS);
        }
        log.info("유저 {} 로그아웃 처리 완료. 엑세스 토큰 블랙리스트 등록: {}, 남은 유효 기간: {}ms", userId,accessToken,TimeUnit.MILLISECONDS);
    }


    //토큰 재발급
    @Override
    public TokenResponse refresh(String refreshToken) {
        //리프레시 토큰 검증: 유효한 토큰인지 + Redis에 저장된 토큰과 일치하는지
        if(!jwtProvider.validateToken(refreshToken)){
            throw new RefreshTokenValidateException("유효하지 않은 리프레시 토큰 입니다.");
        }

        //토큰에서 유저 아이디 추출
        String userId=jwtProvider.getUserIdFromToken(refreshToken);

        //Redis에서 해당 유저의 리프레시 토큰 조회
        String redisKey="refreshToken:"+userId;
        String storedRefreshToken=redisTemplate.opsForValue().get(redisKey);

        //Redis에 저장된 토큰과 요청에서 전달된 토큰이 일치하는지 확인
        if(storedRefreshToken==null || !storedRefreshToken.equals(refreshToken)){
            throw new RefreshTokenValidateException("리프레시 토큰이 일치하지 않거나, 이미 만료되었습니다.");
        }

        //유저 아이디로 유저 조회 -> 토큰 재발급 시점에 유저의 권한이 변경되었을 수 있으므로, 최신 정보를 조회하여 토큰에 반영
        LoginResponse user= accountClient.getUser(userId).getBody();
        String role="ROLE_" +user.userRole();

        //새로운 액세스 토큰과 리프레시 토큰 발급
        String newAccessToken= jwtProvider.createAccessToken(userId,role);
        String newRefreshToken= jwtProvider.createRefreshToken(userId);

        //Redis에 새로운 리프레시 토큰 저장(기존 토큰 덮어쓰기)
        redisTemplate.opsForValue().set(
                redisKey,
                newRefreshToken,
                jwtProperties.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
        log.info("리프레시 토큰 재발급 성공. 유저:{}, 새 액세스 토큰:{}, 새 리프레시 토큰:{}, userId, newAccessToken, newRefreshToken");

        //새로운 토큰을 담은 응답 반환
        return new TokenResponse(newAccessToken,newRefreshToken);
    }
}
