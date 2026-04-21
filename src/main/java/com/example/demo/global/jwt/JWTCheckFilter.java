package com.example.demo.global.jwt;

import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.SuspendedRepository;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.global.exception.CustomJWTException;
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

@Slf4j
public class JWTCheckFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final SuspendedRepository suspendedRepository;

    public JWTCheckFilter(JWTUtil jwtUtil, UserRepository userRepository, SuspendedRepository suspendedRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.suspendedRepository = suspendedRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) return true;

        String path = request.getRequestURI();
<<<<<<< HEAD

        // 🟢 [명단 업데이트] 여기에 포함되면 신분증(토큰) 없어도 통과시켜줍니다.
        if (
                path.equals("/api/login") ||
                        path.startsWith("/oauth2/") ||
                        path.startsWith("/api/oauth2/") ||
                        path.startsWith("/login/oauth2/code/") ||
                        path.startsWith("/api/admin/notice") || // ⭐ 여기 핵심 수정
                        path.contains("/view") ||
                        path.startsWith("/api/member/")
        ) {
=======
        
        // Postman 등의 테스트를 위해 인증 없이 접근해야 하는 경로는 필터 적용 제외
        // 회원가입 관련 경로 모두 허용 (이메일 인증 등)
        if(path.equals("/api/login") ||
           path.equals("/api/logout") || 
           path.equals("/api/member/refresh") ||
           path.startsWith("/api/member/signup") ||
           path.startsWith("/api/member/check-") ||
           path.startsWith("/api/member/email/")) {
>>>>>>> 4a0159ad4cab0c34a5237ae5c714ab85b1e083c7
            return true;
        }

        // 인증이 필요한 경로만 필터 적용
        if (path.startsWith("/api/")) return false;

        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationStr = request.getHeader("Authorization");
            if (authorizationStr == null || !authorizationStr.startsWith("Bearer ")) {
                throw new CustomJWTException("NO_AUTH_HEADER");
            }
            String accessToken = authorizationStr.substring(7);
            Claims claims = jwtUtil.validateToken(accessToken);
            String email = claims.get("email", String.class);

            User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomJWTException("INVALID_USER"));
            suspendedRepository.findByUserIdAndReleasedAtIsNull(user.getId()).ifPresent(s -> { throw new CustomJWTException("SUSPENDED_USER"); });

            MemberDTO memberDTO = new MemberDTO(email, "", "temp_nickname", List.of("USER"));
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(memberDTO, "", memberDTO.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (CustomJWTException e) {
            handleException(response, "ERROR_ACCESS_TOKEN", e.getMessage());
        } catch (Exception e) {
            throw new ServletException(e);
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