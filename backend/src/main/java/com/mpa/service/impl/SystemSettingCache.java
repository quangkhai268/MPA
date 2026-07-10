package com.mpa.service.impl;

import com.mpa.entity.SystemSetting;
import com.mpa.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Bọc SystemSettingRepository qua cache trong bộ nhớ. Các job cảnh báo/chiến dịch có thể gọi
 * getInt/getBoolean/getString (qua SystemSettingService) hàng chục nghìn lần trong 1 lần chạy
 * (1 lần/thẻ) — nếu không cache, mỗi lần gọi là 1 round-trip DB riêng, y hệt vấn đề N+1 đã gặp
 * ở EmailLogRepository/TheDoanhSoSnapshotRepository. Cache bị xóa toàn bộ ngay khi admin lưu
 * cấu hình mới (SystemSettingServiceImpl.updateBatch) để tránh đọc giá trị cũ.
 */
@Component
@RequiredArgsConstructor
public class SystemSettingCache {

    private final SystemSettingRepository repo;

    @Cacheable("systemSettingValue")
    public Optional<String> getValue(String key) {
        return repo.findBySettingKey(key).map(SystemSetting::getSettingValue);
    }

    @CacheEvict(value = "systemSettingValue", allEntries = true)
    public void evictAll() {
        // no-op — chỉ để trigger xóa cache
    }
}
