package com.estaid.auth.service;

import com.estaid.auth.dto.AuthUserResponse;
import com.estaid.auth.dto.LoginRequest;
import com.estaid.common.exception.BusinessException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public static final String SESSION_ADMIN_USERNAME = "LOGIN_ADMIN_USERNAME";

    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AuthService(PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.password:admin1234}") String adminPassword) {
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public AuthUserResponse login(LoginRequest request, HttpSession session) {
        if (!adminUsername.equals(request.username())) {
            throw new BusinessException("관리자 계정 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        String encodedPassword = passwordEncoder.encode(adminPassword);
        if (!passwordEncoder.matches(request.password(), encodedPassword)) {
            throw new BusinessException("관리자 계정 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        session.setAttribute(SESSION_ADMIN_USERNAME, adminUsername);
        return new AuthUserResponse(adminUsername);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}
