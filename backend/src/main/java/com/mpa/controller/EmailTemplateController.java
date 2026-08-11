package com.mpa.controller;

import com.mpa.dto.EmailTemplateRequest;
import com.mpa.dto.EmailTemplateResponse;
import com.mpa.service.EmailTemplateService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cấu hình mẫu email — thuộc nhóm "Cài đặt hệ thống", chỉ ADMIN được truy cập. */
@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class EmailTemplateController {

    private final EmailTemplateService service;

    @GetMapping
    public ApiResponse<List<EmailTemplateResponse>> getAll() {
        try {
            return ApiResponse.ok(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải mẫu email: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<EmailTemplateResponse> update(@PathVariable Integer id,
                                                      @RequestBody EmailTemplateRequest request) {
        try {
            return ApiResponse.ok(service.update(id, request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật mẫu email: " + e.getMessage());
        }
    }
}
