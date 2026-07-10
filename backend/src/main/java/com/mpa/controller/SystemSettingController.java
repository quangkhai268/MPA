package com.mpa.controller;

import com.mpa.dto.SystemSettingRequest;
import com.mpa.dto.SystemSettingResponse;
import com.mpa.service.SystemSettingService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system-settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService service;

    @GetMapping
    public ApiResponse<List<SystemSettingResponse>> getAll() {
        try {
            return ApiResponse.ok(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải cấu hình hệ thống: " + e.getMessage());
        }
    }

    @PutMapping
    public ApiResponse<List<SystemSettingResponse>> updateBatch(@RequestBody List<SystemSettingRequest> requests) {
        try {
            return ApiResponse.ok(service.updateBatch(requests, "admin"));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi lưu cấu hình: " + e.getMessage());
        }
    }
}
