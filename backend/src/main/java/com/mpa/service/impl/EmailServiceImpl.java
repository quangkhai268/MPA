package com.mpa.service.impl;

import com.mpa.service.EmailService;
import com.mpa.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SystemSettingService settingService;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public boolean isEnabled() {
        return settingService.getBoolean("CARD_EMAIL_ENABLED", false);
    }

    @Override
    public String resolveRecipient(String realEmail) {
        String override = settingService.getString("CARD_TEST_EMAIL_OVERRIDE", "");
        return (override != null && !override.isBlank()) ? override.trim() : realEmail;
    }

    @Override
    public String render(String template, Map<String, String> placeholders) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
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
