package com.estaid.auth.filter;

import com.estaid.auth.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 *
 * <p>모든 HTTP 요청에 대해 한 번 실행되며, Authorization 헤더에서 JWT를 추출하고 검증한다.
 * 유효한 토큰이면 SecurityContext에 인증 정보를 설정하여 이후 요청이 통과되도록 한다.</p>
 *
 * <p>처리 흐름:</p>
 * <pre>
 *   요청 수신
 *   → Authorization 헤더에서 "Bearer {token}" 추출
 *   → JwtTokenService로 토큰 파싱 및 검증
 *   → 유효: SecurityContext에 인증 객체 저장 → 다음 필터로 진행
 *   → 무효/없음: SecurityContext 비움 → 다음 필터로 진행 (SecurityConfig에서 401 처리)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    /** Authorization 헤더 이름 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 토큰 접두사 */
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization 헤더에서 토큰 추출
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 2. 토큰 파싱 및 검증
                Claims claims = jwtTokenService.parseToken(token);
                String userId = claims.get("userId", String.class);
                String username = claims.getSubject();

                // 3. 인증 객체 생성 후 SecurityContext에 저장
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 인증 성공: userId={}, username={}", userId, username);

            } catch (JwtException e) {
                // 토큰 위조·만료·형식 오류 → SecurityContext 비움 (이후 SecurityConfig에서 401 반환)
                log.warn("JWT 검증 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 4. 다음 필터로 진행 (인증 여부는 SecurityConfig가 판단)
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출한다.
     *
     * @param request HTTP 요청
     * @return 토큰 문자열 (없으면 null)
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
