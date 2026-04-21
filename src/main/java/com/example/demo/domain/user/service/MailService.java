package com.example.demo.domain.user.service;

import com.example.demo.global.redis.RedisService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private final RedisService redisService; // 인증번호 저장을 위해 사용
    private final SpringTemplateEngine templateEngine; // Thymeleaf 템플릿 엔진 주입

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

    public MimeMessage createMail(String mail, String number) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // 1. 보낸 사람 설정 (발신자 이메일, 수신함에 표시될 이름)
        // InternetAddress 객체를 생성하여 발신자 이름(dead bug)을 명확하게 지정
        helper.setFrom(new InternetAddress(senderEmail, "dead bug"));
        helper.setTo(mail);
        helper.setSubject("[dead bug] 회원가입 이메일 인증 번호");

        // 2. Thymeleaf 템플릿에 데이터 전달
        Context context = new Context();
        context.setVariable("authCode", number);

        // 3. resources/templates/email-auth.html 을 읽어와서 HTML 렌더링
        String htmlContent = templateEngine.process("email-auth", context);

        // 4. 메일 본문에 렌더링된 HTML 설정
        helper.setText(htmlContent, true);

        return message;
    }

    public void sendSimpleMessage(String sendEmail) throws MessagingException {
        String number = createNumber();

        log.info("Sending email to: {} from: {}", sendEmail, senderEmail);

        try {
            MimeMessage message = createMail(sendEmail, number);
            javaMailSender.send(message);

            // Redis에 인증번호 저장 (3분 유효기간)
            redisService.saveAuthCode(sendEmail, number, 3 * 60 * 1000); 
            log.info("Email sent successfully to {}", sendEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}", sendEmail, e);
            throw new IllegalArgumentException("메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    // --- 이메일 변경 인증을 위한 새로운 메서드들 ---

    public MimeMessage createUpdateEmailMail(String mail, String number) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(new InternetAddress(senderEmail, "dead bug"));
        helper.setTo(mail);
        helper.setSubject("[dead bug] 이메일 주소 변경 인증 번호");

        Context context = new Context();
        context.setVariable("authCode", number);

        // 새로운 템플릿 사용 (email-update.html)
        String htmlContent = templateEngine.process("email-update", context);

        helper.setText(htmlContent, true);

        return message;
    }

    public void sendUpdateVerificationMessage(String sendEmail) throws MessagingException {
        String number = createNumber();

        log.info("Sending update verification email to: {} from: {}", sendEmail, senderEmail);

        try {
            MimeMessage message = createUpdateEmailMail(sendEmail, number);
            javaMailSender.send(message);

            redisService.saveAuthCode(sendEmail, number, 3 * 60 * 1000);
            log.info("Update verification email sent successfully to {}", sendEmail);
        } catch (Exception e) {
            log.error("Failed to send update verification email to {}", sendEmail, e);
            throw new IllegalArgumentException("메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    // --- 기존 코드 유지 ---

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