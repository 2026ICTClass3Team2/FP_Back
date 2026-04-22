package com.example.demo.global.oauth2;

import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

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
        String username = memberDTO.getNickname();
        boolean isNewUser = memberDTO.isNewOAuthUser();

        Map<String, Object> attributes = memberDTO.getAttributes();
        log.info("OAuth2 User Attributes: {}", attributes);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);

        String accessToken = jwtUtil.generateToken(claims, 30);
        String refreshToken = jwtUtil.generateToken(claims, 60 * 24 * 7);

        redisService.saveRefreshToken(email, refreshToken, 7);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);

        clearAuthenticationAttributes(request, response);

        String targetUrl = redirectUri.orElse(frontendUrl + "/oauth/callback");
        String encodedUsername = URLEncoder.encode(username != null ? username : "소셜유저", StandardCharsets.UTF_8.name());
        String finalUrl = targetUrl + "?token=" + accessToken + "&username=" + encodedUsername;

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            finalUrl += "&userId=" + user.getId() + "&role=" + user.getRole().name();
        }
        if (isNewUser) {
            finalUrl += "&isNewUser=true";
        }
        
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
