package com.example.demo.global.oauth2;

import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.global.redis.RedisService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final RedisService redisService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value(value = "${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 프론트엔드에서 보낸 redirect_uri 쿠키에서 꺼내오기
        Optional<String> redirectUri = getCookie(request, "redirect_uri")
                .map(Cookie::getValue)
                .map(val -> URLDecoder.decode(val, StandardCharsets.UTF_8));

        MemberDTO memberDTO = (MemberDTO) authentication.getPrincipal();
        String email = memberDTO.getEmail();
        String username = memberDTO.getNickname(); // React에서 필요한 username(또는 닉네임)
        
        // OAuth2 공급자로부터 받은 attributes를 로그로 확인
        Map<String, Object> attributes = memberDTO.getAttributes();
        log.info("OAuth2 User Attributes: {}", attributes);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateToken(claims, 30);
        String refreshToken = jwtUtil.generateToken(claims, 60 * 24 * 7);

        // Redis 저장
        redisService.saveRefreshToken(email, refreshToken, 7);

        // 응답 쿠키에 Refresh Token 추가
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);

        // 쿠키에 담겼던 OAuth2 요청 정보 삭제
        clearAuthenticationAttributes(request, response);

        // 프론트엔드 연동: OAuthCallback 컴포넌트가 있는 라우터로 이동
        // 1순위: 쿠키에 명시된 redirectUri
        // 2순위: 기본 콜백 페이지 URL (/oauth/callback)
        String targetUrl = redirectUri.orElse(frontendUrl + "/oauth/callback"); 
        
        // 한글 이름(username)이 포함되어 있으면 URL Encoding을 거쳐야 에러가 발생하지 않습니다.
        String encodedUsername = URLEncoder.encode(username != null ? username : "소셜유저", StandardCharsets.UTF_8.name());
        
        String finalUrl = targetUrl + "?token=" + accessToken + "&username=" + encodedUsername;
        
        log.info("Redirecting to frontend: {}", finalUrl);
        getRedirectStrategy().sendRedirect(request, response, finalUrl);
    }

    private void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }
}
