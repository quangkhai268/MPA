package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SystemSettingResponse {
    private Integer id;
    private String settingKey;
    private String settingValue;
    private String valueType;
    private String moTa;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
