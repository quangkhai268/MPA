package com.mpa.service.impl;

import com.mpa.dto.SystemSettingRequest;
import com.mpa.dto.SystemSettingResponse;
import com.mpa.entity.SystemSetting;
import com.mpa.repository.SystemSettingRepository;
import com.mpa.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository repo;
    private final SystemSettingCache cache;

    @Override
    public List<SystemSettingResponse> getAll() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(SystemSetting::getSettingKey))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemSettingResponse> updateBatch(List<SystemSettingRequest> requests, String updatedBy) {
        for (SystemSettingRequest req : requests) {
            repo.findBySettingKey(req.getSettingKey()).ifPresent(setting -> {
                setting.setSettingValue(req.getSettingValue());
                setting.setUpdatedAt(LocalDateTime.now());
                setting.setUpdatedBy(updatedBy);
                repo.save(setting);
            });
        }
        cache.evictAll();
        return getAll();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return cache.getValue(key)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> {
                    try {
                        return Integer.parseInt(v.trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return cache.getValue(key)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> Boolean.parseBoolean(v.trim()))
                .orElse(defaultValue);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return cache.getValue(key)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    private SystemSettingResponse toResponse(SystemSetting s) {
        return SystemSettingResponse.builder()
                .id(s.getId())
                .settingKey(s.getSettingKey())
                .settingValue(s.getSettingValue())
                .valueType(s.getValueType())
                .moTa(s.getMoTa())
                .updatedAt(s.getUpdatedAt())
                .updatedBy(s.getUpdatedBy())
                .build();
    }
}
