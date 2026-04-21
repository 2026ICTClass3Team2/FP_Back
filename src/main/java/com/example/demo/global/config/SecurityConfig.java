package com.example.demo.global.config;

import com.example.demo.domain.user.repository.SuspendedRepository;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.global.jwt.JWTCheckFilter;
import com.example.demo.global.handler.ApiLoginFailurerHandler;
import com.example.demo.global.handler.ApiLoginSuccessHandler;
import com.example.demo.global.handler.CustomAccessDeniedHandler;
import com.example.demo.global.handler.CustomAuthenticationEntryPoint;
import com.example.demo.global.handler.CustomLogoutSuccessHandler;
import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.global.oauth2.CustomOAuth2UserService;
import com.example.demo.global.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.demo.global.oauth2.OAuth2LoginFailureHandler;
import com.example.demo.global.oauth2.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JWTUtil jwtUtil;
    private final ApiLoginSuccessHandler apiLoginSuccessHandler;
    private final ApiLoginFailurerHandler apiLoginFailurerHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final UserRepository userRepository;
    private final SuspendedRepository suspendedRepository;

    // OAuth2 의존성 주입
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    // ... 상단 임포트 및 클래스 설정 생략

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(AbstractHttpConfigurer::disable);

        http.formLogin(form -> form
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("pw")
                .successHandler(apiLoginSuccessHandler)
                .failureHandler(apiLoginFailurerHandler)
        );

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 6. 경로별 접근 권한 설정 (핵심: /api/admin/notice/** 추가)
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/login",
                        "/api/user/signup",
                        "/api/oauth2/**",
                        "/api/login/oauth2/code/*",
                        "/api/notices/**",
                        "/api/admin/notice/**" // axious crud api 요청때문에 추가했습니다
                ).permitAll()
                .requestMatchers("/api/mypage/**").authenticated()
                .anyRequest().permitAll()
        );

        // ... 이하 JWT 및 로그아웃 설정 동일
        return http.build();
    }

    // 상원님이 유지하고 싶어 하신 CORS 설정 그대로입니다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration=new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedHeaders(List.of("Authorization","Cache-Control","Content-Type"));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","HEAD"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}