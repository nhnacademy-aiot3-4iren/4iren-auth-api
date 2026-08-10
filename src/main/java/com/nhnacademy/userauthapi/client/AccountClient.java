package com.nhnacademy.userauthapi.client;

import com.nhnacademy.userauthapi.dto.user.UserResponse;
import com.nhnacademy.userauthapi.dto.login.LoginRequest;
import com.nhnacademy.userauthapi.dto.login.LoginResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="4iren-account", path="/api/account")
public interface AccountClient {

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request);

    @GetMapping("/{user-id}")
    ResponseEntity<UserResponse> getUser(
            @PathVariable("user-id") Long userId,
            @RequestHeader("X-USER-ID") Long requesterId
    );
}
