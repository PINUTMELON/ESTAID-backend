package com.estaid.auth.controller;

import com.estaid.auth.dto.AuthUserResponse;
import com.estaid.auth.dto.LoginRequest;
import com.estaid.auth.service.AuthService;
import com.estaid.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthUserResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("로그인 성공", authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok("로그아웃 성공", null);
    }
}
