package com.example.demo.global.jwt;

import com.example.demo.global.exception.CustomJWTException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

@Component
public class JWTUtil {
    private final SecretKey key;

    public JWTUtil(@Value("${jwt.secret}") String secretKey){
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Map<String, Object> valueMap, int min){
        String jwtStr = Jwts.builder()
                .header().type("JWT")
                .and()
                .subject(String.valueOf(valueMap.get("email")))
                .claims(valueMap)
                .issuedAt(Date.from(ZonedDateTime.now().toInstant()))
                .expiration(Date.from(ZonedDateTime.now().plusMinutes(min).toInstant()))
                .signWith(key)
                .compact();
        return jwtStr;
    }

    public Claims validateToken(String token){
        Claims claim = null;
        try{
            claim = Jwts.parser()
                    .verifyWith(key) //시큐릿키값 설정
                    .build()
                    .parseClaimsJws(token) //유효한 토큰값이 아니면 예외발생
                    .getPayload();
        }catch (MalformedJwtException malformedJwtException) { //토큰이 잘못된 형식일때
            throw new CustomJWTException("MalFormed");
        } catch (ExpiredJwtException expiredJwtException) { //유효기간이 만료되었을때
            throw new CustomJWTException("Expired");
        } catch (InvalidClaimException invalidClaimException) { //유효하지 않은 토큰일때
            throw new CustomJWTException("Invalid");
        } catch (JwtException jwtException) {
            throw new CustomJWTException("JWTError");
        }catch(Exception e){
            throw new CustomJWTException("Error");
        }
        return claim;
    }
}