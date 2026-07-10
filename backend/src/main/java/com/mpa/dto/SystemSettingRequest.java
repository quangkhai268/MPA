package com.mpa.dto;

import lombok.Data;

@Data
public class SystemSettingRequest {
    private String settingKey;
    private String settingValue;
}
