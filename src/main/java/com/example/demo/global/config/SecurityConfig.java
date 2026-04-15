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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 1. 세션 사용 안함 (Stateless)
        http.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        // 2. CSRF 비활성화
        http.csrf(AbstractHttpConfigurer::disable);
        
        // 3. 폼 로그인 방식 비활성화 (API 기반) 및 커스텀 로그인 처리
        http.formLogin(form->form
                .loginProcessingUrl("/login") // 요구사항: /api/login 요청 시 처리
                .usernameParameter("email")
                .passwordParameter("pw")
                .successHandler(apiLoginSuccessHandler)
                .failureHandler(apiLoginFailurerHandler)
        );
        
        // 4. CORS 설정 적용
        http.cors(cors->cors.configurationSource(corsConfigurationSource()));

        // 5. OAuth2 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth
                        // 프론트엔드에서 /oauth2/authorization/{provider}?redirect_uri=... 형태로 접근
                        .baseUri("/oauth2/authorization")
                        // 상태 저장을 쿠키 기반으로 변경 (Stateless 하기 위함)
                        .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                )
                .redirectionEndpoint(redirect -> redirect
                        // 소셜 서버 로그인 후 돌아오는 redirect_uri의 base 엔드포인트
                        .baseUri("/login/oauth2/code/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                        // 구글, 카카오 등에서 가져온 사용자 정보를 처리할 클래스
                        .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler) // 소셜 로그인 성공 시 핸들러
                .failureHandler(oAuth2LoginFailureHandler) // 소셜 로그인 실패 시 핸들러
        );
        
        // 6. JWT 확인 필터 추가
        http.addFilterBefore(
                new JWTCheckFilter(jwtUtil, userRepository, suspendedRepository),
                UsernamePasswordAuthenticationFilter.class
        );
        
        // 7. 예외 처리 핸들러 등록
        http.exceptionHandling(ex->
                ex.accessDeniedHandler(customAccessDeniedHandler) // 인가 실패
                  .authenticationEntryPoint(customAuthenticationEntryPoint) // 인증 실패
        );

        // 8. 로그아웃 설정
        http.logout(logout -> logout
                .logoutUrl("/logout") // 로그아웃 경로
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .deleteCookies("refreshToken")
        );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration=new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // 프론트엔드 도메인으로 변경 권장 (예: http://localhost:3000)
        configuration.setAllowedHeaders(List.of("Authorization","Cache-Control","Content-Type"));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","HEAD"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
