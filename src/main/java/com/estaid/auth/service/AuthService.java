package com.estaid.auth.service;

import com.estaid.auth.dto.AuthUserResponse;
import com.estaid.auth.dto.LoginRequest;
import com.estaid.common.exception.BusinessException;
import com.estaid.user.User;
import com.estaid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin1234}")
    private String adminPassword;

    public AuthUserResponse login(LoginRequest request) {
        if (!adminUsername.equals(request.username())) {
            throw new BusinessException("관리자 계정 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        String encodedPassword = passwordEncoder.encode(adminPassword);
        if (!passwordEncoder.matches(request.password(), encodedPassword)) {
            throw new BusinessException("관리자 계정 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        User adminUser = userRepository.findFirstByUsername(adminUsername)
                .orElseThrow(() -> new BusinessException(
                        "로그인할 관리자 사용자 정보가 users 테이블에 없습니다.",
                        HttpStatus.UNAUTHORIZED));

        String accessToken = jwtTokenService.generateToken(adminUser.getUserId(), adminUser.getUsername());
        return new AuthUserResponse(adminUser.getUserId(), adminUser.getUsername(), accessToken, "Bearer");
    }

    public void logout() {
        // Stateless JWT logout is handled client-side by discarding the token.
    }
}
