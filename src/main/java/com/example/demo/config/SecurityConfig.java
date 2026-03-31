package com.example.demo.config;

import com.example.demo.jwt.JWTCheckFilter;
import com.example.demo.handler.ApiLoginFailurerHandler;
import com.example.demo.handler.ApiLoginSuccessHandler;
import com.example.demo.handler.CustomAccessDeniedHandler;
import com.example.demo.jwt.JWTUtil;
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

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.sessionManagement(sessionConfig->{
            sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS);//세션 생성하지 않기
        });
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(form->form
                .loginProcessingUrl("/api/member/login")
                .usernameParameter("email")
                .passwordParameter("pw")
                .successHandler(apiLoginSuccessHandler)
                .failureHandler(apiLoginFailurerHandler)
        );
        http.cors(cors->cors.configurationSource(corsConfigurationSource()));
        http.addFilterBefore(
                new JWTCheckFilter(jwtUtil),
                UsernamePasswordAuthenticationFilter.class
        );
        http.exceptionHandling(ex->
                ex.accessDeniedHandler(customAccessDeniedHandler));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration=new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization","Cache-Control","Content-Type"));
        configuration.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","HEAD"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}