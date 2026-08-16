package com.mpa.service.impl;

import com.mpa.service.EmailService;
import com.mpa.service.SystemSettingService;

import java.util.Map;

public abstract class AbstractEmailService implements EmailService {

    protected final SystemSettingService settingService;

    protected AbstractEmailService(SystemSettingService settingService) {
        this.settingService = settingService;
    }

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
}
