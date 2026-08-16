package com.mpa.service.impl;

import com.mpa.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailServiceImpl extends AbstractEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public SmtpEmailServiceImpl(JavaMailSender mailSender, SystemSettingService settingService) {
        super(settingService);
        this.mailSender = mailSender;
    }

    @Override
    public EmailSendResult send(String to, String subject, String htmlBody) {
        if (!isEnabled()) {
            log.info("Email sending disabled (CARD_EMAIL_ENABLED=false) — skip send to {}", to);
            return EmailSendResult.SKIPPED_DISABLED;
        }
        if (to == null || to.isBlank()) {
            log.warn("Email recipient rỗng — skip send, subject={}", subject);
            return EmailSendResult.SKIPPED_DISABLED;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            return EmailSendResult.SUCCESS;
        } catch (Exception e) {
            log.error("Gửi email thất bại tới {}: {}", to, e.getMessage());
            return EmailSendResult.FAILED;
        }
    }
}
