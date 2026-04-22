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

    public JWTCheckFilter(JWTUtil jwtUtil,
                          UserRepository userRepository,
                          SuspendedRepository suspendedRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.suspendedRepository = suspendedRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        if (request.getMethod().equalsIgnoreCase("OPTIONS")) return true;

        String path = request.getRequestURI();

        // ✅ JWT 없이 접근 가능한 경로 (화이트리스트)
        return path.equals("/api/login")
                || path.equals("/api/logout")
                || path.equals("/api/member/refresh")
                || path.startsWith("/api/member/signup")
                || path.startsWith("/api/member/check-")
                || path.startsWith("/api/member/email/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/api/notice")
                || path.startsWith("/api/admin/notice")// 공개 공지 API라면 유지
                || path.startsWith("/api/api/admin/notice")
                || path.contains("/view");               // 공개 조회 페이지
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authorizationStr = request.getHeader("Authorization");

            if (authorizationStr == null || !authorizationStr.startsWith("Bearer ")) {
                throw new CustomJWTException("NO_AUTH_HEADER");
            }

            String accessToken = authorizationStr.substring(7);
            Claims claims = jwtUtil.validateToken(accessToken);

            String email = claims.get("email", String.class);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new CustomJWTException("INVALID_USER"));

            suspendedRepository.findByUserIdAndReleasedAtIsNull(user.getId())
                    .ifPresent(s -> {
                        throw new CustomJWTException("SUSPENDED_USER");
                    });

            MemberDTO memberDTO = new MemberDTO(
                    email,
                    "",
                    "temp_nickname",
                    List.of("USER")
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            memberDTO,
                            "",
                            memberDTO.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (CustomJWTException e) {
            handleException(response, "ERROR_ACCESS_TOKEN", e.getMessage());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void handleException(HttpServletResponse response,
                                 String errorKey,
                                 String errorMessage) throws IOException {

        Gson gson = new Gson();

        String jsonStr = gson.toJson(
                Map.of(
                        "error", errorKey,
                        "message", errorMessage
                )
        );

        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        PrintWriter pw = response.getWriter();
        pw.println(jsonStr);
        pw.close();
    }
}