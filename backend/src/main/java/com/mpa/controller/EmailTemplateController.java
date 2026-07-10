package com.mpa.controller;

import com.mpa.dto.EmailTemplateRequest;
import com.mpa.dto.EmailTemplateResponse;
import com.mpa.service.EmailTemplateService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
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
