package com.example.demo.domain.user.service;

import com.example.demo.global.redis.RedisService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private final RedisService redisService; // 인증번호 저장을 위해 사용

    @Value("${spring.mail.username}")
    private String senderEmail;

    // 인증번호 생성
    public String createNumber() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 6; i++) { // 인증번호 6자리
            key.append(random.nextInt(10));
        }
        return key.toString();
    }

    public MimeMessage createMail(String mail, String number) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail);
        helper.setTo(mail);
        helper.setSubject("[Final Project] 회원가입 이메일 인증 번호");

        String body = "";
        body += "<h3>요청하신 인증 번호입니다.</h3>";
        body += "<h1>" + number + "</h1>";
        body += "<h3>감사합니다.</h3>";

        helper.setText(body, true);

        return message;
    }

    public String sendSimpleMessage(String sendEmail) throws MessagingException {
        String number = createNumber();

        MimeMessage message = createMail(sendEmail, number);
        
        try {
            javaMailSender.send(message);
            // Redis에 인증번호 저장 (3분 유효기간)
            redisService.saveAuthCode(sendEmail, number, 3 * 60 * 1000); 
            log.info("Email sent successfully to {}", sendEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}", sendEmail, e);
            throw new IllegalArgumentException("메일 발송에 실패했습니다.");
        }

        return number;
    }

    // 인증번호 검증
    public boolean verifyAuthCode(String email, String code) {
        String savedCode = redisService.getAuthCode(email);
        
        if (savedCode != null && savedCode.equals(code)) {
            redisService.deleteAuthCode(email); // 인증 성공 후 삭제
            return true;
        }
        return false;
    }
}
