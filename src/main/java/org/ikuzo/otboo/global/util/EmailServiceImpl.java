package org.ikuzo.otboo.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendTemporaryPassword(String email, String name, String tempPassword) {
        log.debug("임시 비밀번호 이메일 전송 시작: to={}", email);

        // 이메일 내용 작성
        String subject = "[OTBOO] 임시 비밀번호 발급 안내";
        String content = String.format(
            """
            안녕하세요, %s님.
            
            요청하신 임시 비밀번호가 발급되었습니다.
            
            임시 비밀번호: %s
            
            로그인 후 반드시 비밀번호를 변경해주세요.
            
            감사합니다.
            OTBOO 드림
            """,
            name,
            tempPassword
        );

        // 이메일 전송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);
        message.setFrom(fromEmail);  // 발신자 이메일 (설정 필요)

        mailSender.send(message);
        log.info("임시 비밀번호 이메일 전송 완료: to={}", email);
    }
}