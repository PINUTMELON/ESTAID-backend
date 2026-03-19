package com.estaid.common.config;

import com.estaid.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 *
 * <p>인증 방식: Stateless JWT</p>
 *
 * <p>허용 경로 (인증 없이 접근 가능):</p>
 * <ul>
 *   <li>POST /auth/login  - 로그인 (토큰 발급)</li>
 *   <li>POST /auth/logout - 로그아웃 (Stateless이므로 클라이언트가 토큰 폐기)</li>
 *   <li>GET  /api/content/** - 공개 조회 API (갤러리 등)</li>
 * </ul>
 *
 * <p>인증 필요 경로:</p>
 * <ul>
 *   <li>/api/projects/**, /api/characters/**, /api/plots/**</li>
 *   <li>/api/images/**, /api/videos/** - 이미지·영상 생성 API</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 허용
                .requestMatchers("/auth/login", "/auth/logout").permitAll()
                .requestMatchers("/api/content/**").permitAll()  // 공개 조회 API
                // 나머지 모든 API는 JWT 인증 필요
                .anyRequest().authenticated()
            )
            // UsernamePasswordAuthenticationFilter 앞에 JWT 필터 삽입
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
