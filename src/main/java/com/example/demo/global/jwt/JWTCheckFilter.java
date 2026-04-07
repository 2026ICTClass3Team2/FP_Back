package com.example.demo.global.jwt;

import com.example.demo.user.dto.MemberDTO;
import com.example.demo.global.exception.CustomJWTException;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

// 요청 당 한번만 동작되는 필터 만들기
public class JWTCheckFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;

    public JWTCheckFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    // true가 반환되면 필터가 수행되지 않음 / false가 리턴되면 필터를 수행함
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 요청 uri
        String uri= request.getRequestURI(); //  /api/todos/insert
        if(uri.startsWith("/api/todos")){ //필터가 수행되어야 하는 경로
            return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationStr = request.getHeader("Authorization");
            if (authorizationStr == null || !authorizationStr.startsWith("Bearer ")) {
                throw new CustomJWTException("NO_AUTH_HEADER");
            }
            //"Bearer AB123XXXXXXX";  => "Bearer JWT값"
            String accessToken = authorizationStr.substring(7);
            Claims claims = jwtUtil.validateToken(accessToken);
            String email=claims.getSubject();
            List<String> roleNames = claims.get("roleName",List.class);
            //사용자 정보를 꺼내와서 UserDetails객체(User객체)에 저장하기
            MemberDTO memberDTO = new MemberDTO(email, "", null,false, roleNames);
            ///////  인증된 사용자 정보를 스프링시큐리티 컨텍스트에 등록하기   ///////////////
            //UsernamePasswordAuthenticationToken : 사용자이름과 비밀번호를 기반으로 인증정보를 나타내는 클래스
            UsernamePasswordAuthenticationToken authenticationToken=
                    new UsernamePasswordAuthenticationToken(
                            memberDTO, //principal
                            "", // 비밀번호는 담을 필요가 없다.(보안상 값을 넣는건 안좋음)=>더미데이터를 넣거나 빈문자 넣기
                            memberDTO.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            // jwt로 인증하지 못하면 클라이언트에 오류정보를 json으로 응답하기
            Gson gson=new Gson();
            String jsonStr=gson.toJson(Map.of("error","ERROR_ACCESS_TOKEN"));
            response.setContentType("application/json;charset=utf-8");
            PrintWriter pw= response.getWriter();
            pw.println(jsonStr);
            pw.close();
        }
    }
}