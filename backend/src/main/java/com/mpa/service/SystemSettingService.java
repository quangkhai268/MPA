package com.mpa.service;

import com.mpa.dto.SystemSettingRequest;
import com.mpa.dto.SystemSettingResponse;

import java.util.List;

public interface SystemSettingService {

    List<SystemSettingResponse> getAll();

    List<SystemSettingResponse> updateBatch(List<SystemSettingRequest> requests, String updatedBy);

    int getInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    String getString(String key, String defaultValue);
}
