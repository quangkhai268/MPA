package com.mpa.service.impl;

import com.mpa.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "sendify")
public class SendifyEmailServiceImpl extends AbstractEmailService {

    private final RestTemplate restTemplate;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.sendify.api-key}")
    private String apiKey;

    @Value("${app.sendify.base-url}")
    private String baseUrl;

    public SendifyEmailServiceImpl(RestTemplate restTemplate, SystemSettingService settingService) {
        super(settingService);
        this.restTemplate = restTemplate;
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
        if (apiKey == null || apiKey.isBlank()) {
            log.error("SENDIFY_API_KEY chưa được cấu hình — skip send tới {}", to);
            return EmailSendResult.FAILED;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "from", fromName + " <" + fromAddress + ">",
                    "to", to,
                    "subject", subject,
                    "html", htmlBody
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                Object id = response.getBody() != null ? response.getBody().get("id") : null;
                log.info("Sendify đã nhận email tới {} (id={})", to, id);
                return EmailSendResult.SUCCESS;
            }
            log.error("Sendify trả về status không mong đợi {} khi gửi tới {}", response.getStatusCode(), to);
            return EmailSendResult.FAILED;
        } catch (HttpStatusCodeException e) {
            log.error("Sendify lỗi {} khi gửi tới {}: {}", e.getStatusCode(), to, e.getResponseBodyAsString());
            return EmailSendResult.FAILED;
        } catch (Exception e) {
            log.error("Gửi email qua Sendify thất bại tới {}: {}", to, e.getMessage());
            return EmailSendResult.FAILED;
        }
    }
}
