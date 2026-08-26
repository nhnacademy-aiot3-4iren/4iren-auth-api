package com.nhnacademy.userauthapi.config;

import com.nhnacademy.userauthapi.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

//토큰 생성, 검증, 파싱을 담당하는 클래스
@Slf4j
@Component
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    @Autowired
    public JwtProvider(JwtProperties jwtProperties) { //JwtProvider라는 객체를 처음 만들때 이 생성자를 불러서 jwtProperties를 주입받아 준비해주고
        this.jwtProperties=jwtProperties;
        byte[] keyBytes=Decoders.BASE64.decode(jwtProperties.getSecretKey());
        this.secretKey=Keys.hmacShaKeyFor(keyBytes);

    }

    //access token 생성
    public String createAccessToken(Long userId, String loginId, String role){
        long now=System.currentTimeMillis();
        Date accessTokenExpiration=new Date(now+jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .id(java.util.UUID.randomUUID().toString())
                .claim("login-id", loginId)
                .claim("role",role)
                .issuedAt(new Date(now))
                .expiration(accessTokenExpiration)
                .signWith(secretKey)
                .compact();
    }


    //refresh token 생성
    public String createRefreshToken(Long userId){
        long now=System.currentTimeMillis();
        Date refreshTokenExpiration=new Date(now +jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date(now))
                .expiration(refreshTokenExpiration)
                .signWith(secretKey)
                .compact();
    }


    //토큰에서 클레임(정보) 추출
    public Claims getClaims(String token){
        try {
            // 만료되지 않은 토큰에서 클레임 빼내기
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서 클레임 빼내기
            return e.getClaims();
        }

    }
    //***** 클레임 : JWT 표준 스펙용어. Payload에 담기는 key-value형태의 정보 한조각을 공식적으로 클레임이라고 부름.

    //토큰에서 userId 추출
    public Long getUserIdFromToken(String token){
        return Long.valueOf(getClaims(token).getSubject());
    }

    //토큰에서 로그인 ID 추출
    public String getLoginIdFromToken(String token) {
        return getClaims(token).get("login-id", String.class);
    }

    //토큰에서 Role 추출
    public String getRoleFromToken(String token){
        return getClaims(token).get("role",String.class);
    }

    //토큰에서 남은 유효기간 계산
    public Long getRemainingTime(String token){
        try{
            Date expiration=getClaims(token).getExpiration();
            long now=new Date().getTime();
            return expiration.getTime()-now;
        }catch (JwtException e){
            return 0L;
        }
    }

    //토큰 검증-> 유효한 토큰인지, 만료되었는지 검사
    public boolean validateToken(String token){
        try{
            // jwt 직접 파싱 시도
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;    // 성공 시, 유효한 토큰
        }catch (JwtException | IllegalArgumentException e){
            log.warn("Invalid JWT token:{}",e.getMessage());
            return false;   // 실패 시, 훼손되거나 만료된 토큰
        }
    }

    //토큰에서 JTI(고유 ID) 추출
    public String getJtiFromToken(String token){
        return getClaims(token).getId();
    }

}
