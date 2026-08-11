package com.mpa.controller;

import com.mpa.dto.JobRunResult;
import com.mpa.service.CardNotificationService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Trigger job gửi email cảnh báo thẻ thật — chỉ ADMIN được chạy. */
@RestController
@RequestMapping("/api/card-notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class CardNotificationController {

    private final CardNotificationService service;

    @PostMapping("/chua-kich-hoat/run")
    public ApiResponse<JobRunResult> runChuaKichHoat() {
        try {
            return ApiResponse.ok(service.processChuaKichHoat());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi chạy job chưa kích hoạt: " + e.getMessage());
        }
    }

    @PostMapping("/chua-psgd/run")
    public ApiResponse<JobRunResult> runChuaPsgd() {
        try {
            return ApiResponse.ok(service.processChuaPsgd());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi chạy job chưa PSGD: " + e.getMessage());
        }
    }

    /** Gửi thử đúng 1 email "chưa kích hoạt" để kiểm tra mẫu email/SMTP. */
    @PostMapping("/chua-kich-hoat/test-send-one")
    public ApiResponse<String> testSendChuaKichHoatOne() {
        try {
            return ApiResponse.ok(service.testSendChuaKichHoatOne().name());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi gửi thử email: " + e.getMessage());
        }
    }
}
