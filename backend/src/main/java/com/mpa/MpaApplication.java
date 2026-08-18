package com.mpa;

import org.apache.poi.util.IOUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class MpaApplication {
    public static void main(String[] args) {
        // Mặc định POI giới hạn 1 bản ghi nội bộ (VD shared-strings table) tối đa ~100MB khi
        // giải nén .xlsx, để chặn zip-bomb. File MPA lũy kế nội bộ (nguồn tin cậy, không phải
        // upload công khai) có thể vượt ngưỡng này với dữ liệu lớn — nâng lên 300MB thay vì
        // tắt hẳn giới hạn.
        IOUtils.setByteArrayMaxOverride(300_000_000);
        SpringApplication.run(MpaApplication.class, args);
    }
}
