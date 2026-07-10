package com.mpa.service;

import java.util.Map;

public interface EmailService {

    /** Công tắc tổng CARD_EMAIL_ENABLED. */
    boolean isEnabled();

    /**
     * Trả về CARD_TEST_EMAIL_OVERRIDE nếu có giá trị (mọi email thật sẽ được redirect về đây),
     * ngược lại trả về địa chỉ thật của khách hàng.
     */
    String resolveRecipient(String realEmail);

    /** Thay các placeholder dạng {{key}} trong template bằng giá trị tương ứng. */
    String render(String template, Map<String, String> placeholders);

    /**
     * Gửi email. Không bao giờ throw exception ra ngoài — trả kết quả để caller tự ghi email_log.
     * Nếu công tắc tổng đang TẮT, hoặc "to" rỗng, trả về SKIPPED_DISABLED mà không gọi SMTP.
     */
    EmailSendResult send(String to, String subject, String htmlBody);

    enum EmailSendResult {
        SUCCESS, FAILED, SKIPPED_DISABLED
    }
}
