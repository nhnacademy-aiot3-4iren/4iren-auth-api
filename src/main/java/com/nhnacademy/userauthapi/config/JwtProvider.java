package com.nhnacademy.userauthapi.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

//토큰 생성, 검증, 파싱을 담당하는 클래스
@Slf4j
@Component
public class JwtProvider {
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    @Autowired
    public JwtProvider(JwtProperties jwtProperties) { //JwtProvider라는 객체를 처음 만들때 이 생성자를 불러서 jwtProperties를 주입받아 준비해주고

        this.jwtProperties = jwtProperties; //나중에 Access,Refresh토큰들 만들때 만료시간 getAccessTokenExpiration() 같은거 갖다 쓰기 위함임
        byte[] keyBytes= Decoders.BASE64.decode(jwtProperties.getSecretKey()); // SecretKey를 바로 쓸 수 있게 초기화(변환) 작업까지 한번에 처리해줌
        this.secretKey = Keys.hmacShaKeyFor(keyBytes); //바이트 배열 가지고 HMAC-SHA 알고리즘 전용 SecretKey 자바 객체를 생성함
    }

    //access token 생성
    public String createAccessToken(String userId, String role){
        long now=System.currentTimeMillis(); // 현재 시간을 밀리초 단위 숫자로 가져옴
        Date accessTokenExpiration=new Date(now + jwtProperties.getAccessTokenExpiration());
        // 현재 시간에 yml에서 설정했던 1시간을 더함

        return Jwts.builder()
                .subject(userId)    //토큰의 주체(userId)
                .claim("role",role) //토큰에 추가적인 정보(클레임)로 역할(role) 저장
                .issuedAt(new Date(now)) //토큰 발행시간
                .expiration(accessTokenExpiration) //토큰 만료 시간
                .signWith(secretKey)    //토큰 서명에 사용할 비밀 키 설정
                .compact(); //토큰 생성 및 직렬화 하여 문자열로 반환
    }

    //refresh token 생성
    public String createRefreshToken(String userId) {
        long now=System.currentTimeMillis();
        Date refreshTokenExpiration= new Date(now+jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(refreshTokenExpiration)
                .signWith(secretKey)
                .compact();
    }
//
//    public String getUserIdFromToken(String accessTocken) {
//    }
}
