package com.example.demo.global.oauth2;

import com.example.demo.domain.user.dto.MemberDTO;
import com.example.demo.global.jwt.JWTUtil;
import com.example.demo.global.redis.RedisService;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 프론트엔드에서 보낸 redirect_uri 쿠키에서 꺼내오기
        Optional<String> redirectUri = getCookie(request, "redirect_uri")
                .map(Cookie::getValue)
                .map(val -> URLDecoder.decode(val, StandardCharsets.UTF_8));

        MemberDTO memberDTO = (MemberDTO) authentication.getPrincipal();
        String email = memberDTO.getEmail();
        String nickname = memberDTO.getNickname();
        
        // OAuth2 공급자로부터 받은 attributes를 로그로 확인하고 응답에 포함시킵니다.
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

        // 만약 redirect_uri가 쿠키에 명시되어 있다면 해당 주소로 리다이렉트 (보통 프론트에서 넘어왔을 때)
        if (redirectUri.isPresent()) {
            String targetUrl = redirectUri.get();
            String finalUrl = targetUrl + "?token=" + accessToken;
            getRedirectStrategy().sendRedirect(request, response, finalUrl);
            return;
        }
        
        // 프론트엔드가 따로 없고 그냥 백엔드 API에서 브라우저로 화면을 띄웠을 때 JSON 출력되게 함
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);
        responseData.put("email", email);
        responseData.put("nickname", nickname);
        responseData.put("message", "OAuth2 Login Success");
        
        // 디버깅을 위해 응답에 attributes 전체를 포함시킵니다. (프로덕션에서는 제외 권장)
        responseData.put("attributes", attributes);

        response.setContentType("application/json;charset=utf-8");
        PrintWriter pw = response.getWriter();
        pw.print(new Gson().toJson(responseData));
        pw.close();
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
