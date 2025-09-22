package JYBank.JYBank.support.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpMailService{

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")    private String from;
    @Value("${app.mail.brand:JYBank}") private String brand;

    @Async // 비동기 발송(원하면 제거)
    public void sendPasswordResetLink(String toEmail, String link) {
        String subject = "[" + brand + "] 비밀번호 재설정 안내";
        String html = """
            <div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;line-height:1.6">
              <h2 style="margin:0 0 12px">비밀번호 재설정</h2>
              <p>아래 버튼을 눌러 비밀번호 재설정을 진행해 주세요. 이 링크는 15분 후 만료됩니다.</p>
              <p style="margin:20px 0">
                <a href="%s" style="background:#1a73e8;color:#fff;padding:10px 16px;text-decoration:none;border-radius:6px;display:inline-block">
                  비밀번호 재설정하기
                </a>
              </p>
              <p>동작하지 않으면 링크를 복사해 브라우저에 붙여넣기:<br/>
                 <a href="%s">%s</a></p>
              <hr style="border:none;border-top:1px solid #eee;margin:24px 0"/>
              <small>요청하지 않았다면 이 메일을 무시하세요.</small>
            </div>
            """.formatted(link, link, link);
        String text = """
            비밀번호 재설정 링크(15분 내 유효)
            %s
            요청하지 않았다면 이 메일을 무시하세요.
            """.formatted(link);
        send(toEmail, subject, html, text);
    }

    @Async
    public void sendEmailVerificationCode(String toEmail, String code) {
        String subject = "[" + brand + "] 이메일 인증 코드";
        String html = """
            <div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;line-height:1.6">
              <h2 style="margin:0 0 12px">이메일 인증</h2>
              <p>아래 6자리 코드를 입력해 인증을 완료해 주세요. (유효시간 10분)</p>
              <div style="font-size:24px;font-weight:700;letter-spacing:3px;margin:16px 0">%s</div>
              <hr style="border:none;border-top:1px solid #eee;margin:24px 0"/>
              <small>요청하지 않았다면 이 메일을 무시하세요.</small>
            </div>
            """.formatted(code);
        String text = "이메일 인증 코드: %s (10분 유효)".formatted(code);
        send(toEmail, subject, html, text);
    }

    private void send(String to, String subject, String html, String text) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html); // text/plain + text/html
            mailSender.send(msg);
            log.info("Mail sent: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("Mail send failed: to={}, subject={}, err={}", to, subject, e.getMessage(), e);
            throw new IllegalStateException("메일 발송 실패");
        }
    }
}
