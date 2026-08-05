package com.nhnacademy.userauthapi.controller;

import com.nhnacademy.userauthapi.config.JwtProperties;
import com.nhnacademy.userauthapi.dto.token.TokenResponse;

import com.nhnacademy.userauthapi.dto.login.LoginRequest;
import com.nhnacademy.userauthapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//로그인, 로그아웃, 토큰 갱신 등의 인증 관련 API를 제공하는 컨트롤러
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req)
    {
        TokenResponse tokenResponse=authService.login(req);

        //로그인 성공-> 엑세스 토큰은 응답 본문, 리프레시 토큰은 HttpOnly 쿠키로 전달
        ResponseCookie refreshTokenCookie=ResponseCookie.from("refreshToken",tokenResponse.refreshToken())
                .httpOnly(true)                     //자바 스크립트에서 접근 불가능하도록 설정
                .secure(false)                      // HTTPS환경: true, 개발환경:false
                .path("/")                          //모든 경로에서 쿠키가 전송되도록 설정
                .maxAge(jwtProperties.getRefreshTokenExpiration()/1000) // 리프레쉬 토큰 유지 기간동안 유지
                .build();

        //엑세스 토큰만 응답 본문에 담아서 전달
        TokenResponse resp=new TokenResponse(tokenResponse.accessToken(), null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,refreshTokenCookie.toString()) //리프레시 토큰을 HttpOnly 쿠키로 설정
                .body(resp); //엑세스 토큰은 응답 본문에 담아서 전달
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader)
    {
        //Authorization 헤더에서 "Bearer" 접두어를 제거하여 엑세스 토큰 추출
        if(authHeader==null||!authHeader.startsWith("Bearer")){
            return ResponseEntity.badRequest().build();
            //Authorization 헤더가 없거나 "Bearer"로 시작하지 않는 경우 잘못된 요청으로 처리
        }
        String accessToken=authHeader.substring(7); //"Bearer" 접두어 제거

        authService.logout(accessToken);

        ResponseCookie deleteCookie=ResponseCookie.from("refreshToken","")
                .httpOnly(true)
                .secure(false) //HTTPS 환경: true, 개발 환경: false
                .path("/")
                .maxAge(0) //즉시 만료되도록 설정
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString()) //리프레시 토큰 쿠키
                .build();
    }

    //리프레시 토큰을 이용한 엑세스 토큰 갱신-> 리프레시 토큰은 HttpOnly 쿠키에서 읽어서 사용
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@CookieValue(value = "refreshToken",required = false) String refreshToken)
    {
        if(refreshToken==null){
            return ResponseEntity.status(401).build();
        }
        TokenResponse tokenResponse=authService.refresh(refreshToken);

        TokenResponse resp=new TokenResponse(tokenResponse.accessToken(),null);

        return ResponseEntity.ok().body(resp);
    }

}
