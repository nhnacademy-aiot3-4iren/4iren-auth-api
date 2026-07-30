package com.nhnacademy.userauthapi.client;

import com.nhnacademy.userauthapi.dto.login.LoginRequest;
import com.nhnacademy.userauthapi.dto.login.LoginResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="4iren-account", path="/api/account")
public interface AccountClient {

    @GetMapping("/login")
    ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request);

    @GetMapping("/{userId}")
    ResponseEntity<LoginResponse> getUser(@PathVariable("userId") String userId);
}
