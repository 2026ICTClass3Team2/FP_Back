package com.example.demo.global.oauth2;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
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
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 Login Failed: {}", exception.getMessage());
        
        Optional<String> redirectUri = getCookie(request, "redirect_uri")
                .map(Cookie::getValue)
                .map(val -> URLDecoder.decode(val, StandardCharsets.UTF_8));

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        // 프론트엔드 연동 상태라면 리다이렉트
        if (redirectUri.isPresent()) {
            String targetUrl = redirectUri.get() + "?error=" + exception.getLocalizedMessage();
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // 백엔드 단독 테스트(브라우저) 환경이라면 JSON 형태로 에러 출력
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("error", "OAuth2 Login Failed");
        responseData.put("message", exception.getLocalizedMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=utf-8");
        PrintWriter pw = response.getWriter();
        pw.print(new Gson().toJson(responseData));
        pw.close();
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
