package com.example.demo.handler;

import com.example.demo.user.dto.MemberDTO;
import com.example.demo.jwt.JWTUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApiLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JWTUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        MemberDTO dto=(MemberDTO)authentication.getPrincipal();

        Map<String,Object> claims=dto.getClaims();

        String accessToken= jwtUtil.generateToken(claims,5);
        String refreshToken= jwtUtil.generateToken(claims,60*2);//2시간
        claims.put("accessToken",accessToken);
        claims.put("refreshToken",refreshToken);

        Gson gson=new Gson();
        String jsonStr=gson.toJson(claims);
        response.setContentType("application/json;charset=utf-8");
        PrintWriter pw= response.getWriter();
        pw.println(jsonStr);
        pw.close();
    }
}