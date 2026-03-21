package com.estaid.auth.service;

import com.estaid.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    public String requireCurrentUserId(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException("Authorization Bearer 토큰이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new BusinessException("Authorization Bearer 토큰이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }

        try {
            Claims claims = jwtTokenService.parseToken(token);
            String userId = claims.get("userId", String.class);
            if (userId == null || userId.isBlank()) {
                throw new BusinessException("토큰에 사용자 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
            }
            return userId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }
    }
}
