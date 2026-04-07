package com.example.demo.global.config;

import com.example.demo.global.jwt.JWTCheckFilter;
import com.example.demo.global.handler.ApiLoginFailurerHandler;
import com.example.demo.global.handler.ApiLoginSuccessHandler;
import com.example.demo.global.handler.CustomAccessDeniedHandler;
import com.example.demo.global.handler.CustomAuthenticationEntryPoint;
import com.example.demo.global.handler.CustomLogoutSuccessHandler;
import com.example.demo.global.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        // 1. 세션 사용 안함 (Stateless)
        http.sessionManagement(sessionConfig->{
            sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });
        
        // 2. CSRF 비활성화
        http.csrf(AbstractHttpConfigurer::disable);
        
        // 3. 폼 로그인 방식 비활성화 (API 기반) 및 커스텀 로그인 처리
        http.formLogin(form->form
                .loginProcessingUrl("/api/login") // 요구사항: /api/login 요청 시 처리
                .usernameParameter("email")
                .passwordParameter("pw")
                .successHandler(apiLoginSuccessHandler)
                .failureHandler(apiLoginFailurerHandler)
        );
        
        // 4. CORS 설정 적용
        http.cors(cors->cors.configurationSource(corsConfigurationSource()));
        
        // 5. JWT 확인 필터 추가
        http.addFilterBefore(
                new JWTCheckFilter(jwtUtil),
                UsernamePasswordAuthenticationFilter.class
        );
        
        // 6. 예외 처리 핸들러 등록
        http.exceptionHandling(ex->
                ex.accessDeniedHandler(customAccessDeniedHandler) // 인가 실패
                  .authenticationEntryPoint(customAuthenticationEntryPoint) // 인증 실패
        );

        // 7. 로그아웃 설정
        http.logout(logout -> logout
                .logoutUrl("/api/logout") // 로그아웃 경로
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .deleteCookies("refreshToken")
        );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration=new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // 프론트엔드 도메인으로 변경 권장 (예: http://localhost:3000)
        configuration.setAllowedHeaders(Arrays.asList("Authorization","Cache-Control","Content-Type"));
        configuration.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","HEAD"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
