package com.example.demo.global.jwt;

import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.global.exception.CustomJWTException;
import com.example.demo.global.redis.RedisService;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

// 요청 당 한번만 동작되는 필터
@Slf4j
public class JWTCheckFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final RedisService redisService;

    public JWTCheckFilter(JWTUtil jwtUtil, RedisService redisService) {
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 필터링을 생략할 경로 설정
        String path = request.getRequestURI();
        
        // Postman 등의 테스트를 위해 인증 없이 접근해야 하는 경로는 필터 적용 제외
        // 회원가입 관련 경로 모두 허용 (이메일 인증 등)
        if(path.equals("/api/login") ||
           path.equals("/api/logout") || 
           path.equals("/api/member/refresh") ||
           path.startsWith("/api/member/signup") ||
           path.startsWith("/api/member/check-") ||
           path.startsWith("/api/member/email/")) {
            return true;
        }

        // 인증이 필요한 경로만 필터 적용 (필요에 따라 변경)
        if(path.startsWith("/api/")) {
            return false;
        }
        
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 헤더에서 Access Token 추출
            String authorizationStr = request.getHeader("Authorization");
            if (authorizationStr == null || !authorizationStr.startsWith("Bearer ")) {
                log.error("No valid Authorization header found");
                throw new CustomJWTException("NO_AUTH_HEADER");
            }
            
            // "Bearer " 문자열을 제외한 순수 JWT 추출
            String accessToken = authorizationStr.substring(7);
            
            // 블랙리스트 확인 (토큰 폐기 전략 적용)
            if (redisService.isBlackList(accessToken)) {
                log.error("Access Token is in blacklist");
                throw new CustomJWTException("LOGGED_OUT_TOKEN");
            }

            log.info("Access Token Validation: {}", accessToken);
            
            // Token 검증 및 Claims 추출
            Claims claims = jwtUtil.validateToken(accessToken);
            String email = claims.get("email", String.class);
            
            // roleName이 존재하는 경우 처리 로직 (없다면 수정 필요)
            List<String> roleNames = null;
            if(claims.get("roleName") != null) {
               roleNames = claims.get("roleName", List.class);
            } else {
                roleNames = List.of("USER");
            }

            // 사용자 정보를 MemberDTO에 저장
            MemberDTO memberDTO = new MemberDTO(email, "", "temp_nickname", roleNames);
            
            // 인증 객체 생성 및 SecurityContext 등록
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            memberDTO, 
                            "", 
                            memberDTO.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
            
        } catch (CustomJWTException e) {
            // JWT 관련 커스텀 예외 처리
            log.error("JWT Exception: {}", e.getMessage());
            handleException(response, "ERROR_ACCESS_TOKEN", e.getMessage());
        } catch (Exception e) {
            // 그 외 일반 예외 처리
            log.error("Internal Server Error: {}", e.getMessage(), e);
            handleException(response, "ERROR_SERVER", "Internal Server Error");
        }
    }

    private void handleException(HttpServletResponse response, String errorKey, String errorMessage) throws IOException {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(Map.of("error", errorKey, "message", errorMessage));
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        PrintWriter pw = response.getWriter();
        pw.println(jsonStr);
        pw.close();
    }
}
