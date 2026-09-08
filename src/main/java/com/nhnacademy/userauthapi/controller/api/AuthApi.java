package com.nhnacademy.userauthapi.controller.api;

import com.nhnacademy.userauthapi.dto.login.LoginRequest;
import com.nhnacademy.userauthapi.dto.token.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Auth", description = "인증(로그인, 로그아웃, 토큰 갱신) 관련 API")
public interface AuthApi {

    @Operation(summary = "로그인", description = "사용자의 로그인 ID와 비밀번호를 받아 검증 후 엑세스 토큰(응답 본문)과 리프레시 토큰(HttpOnly 쿠키)을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식이 잘못됨 (Validation 실패)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패 (존재하지 않는 아이디 또는 비밀번호 불일치 등)", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    ResponseEntity<TokenResponse> login(
            @Parameter(description = "로그인 요청 정보 (아이디, 비밀번호)", required = true)
            @Valid @RequestBody LoginRequest req
    );

    @Operation(summary = "로그아웃", description = "현재 발급된 엑세스 토큰을 받아 로그아웃 처리하고 리프레시 토큰 쿠키를 삭제(만료)합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "Authorization 헤더가 없거나 형식이 잘못됨", content = @Content),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    ResponseEntity<Void> logout(
            @Parameter(description = "Authorization 헤더 (Bearer 엑세스 토큰)", required = true)
            @RequestHeader("Authorization") String authHeader
    );

    @Operation(summary = "토큰 갱신", description = "HttpOnly 쿠키로 전달된 리프레시 토큰을 이용해 새로운 엑세스 토큰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰이 없거나 유효하지 않음", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    ResponseEntity<TokenResponse> refreshToken(
            @Parameter(description = "리프레시 토큰 (HttpOnly 쿠키)", required = false)
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    );
}
