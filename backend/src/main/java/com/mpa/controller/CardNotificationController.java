package com.mpa.controller;

import com.mpa.dto.JobRunResult;
import com.mpa.service.CardNotificationService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/card-notifications")
@RequiredArgsConstructor
public class CardNotificationController {

    private final CardNotificationService service;

    @PostMapping("/chua-kich-hoat/run")
    public ApiResponse<JobRunResult> runChuaKichHoat(@RequestParam(defaultValue = "true") boolean testMode) {
        try {
            return ApiResponse.ok(service.processChuaKichHoat(testMode));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi chạy job chưa kích hoạt: " + e.getMessage());
        }
    }

    @PostMapping("/chua-psgd/run")
    public ApiResponse<JobRunResult> runChuaPsgd(@RequestParam(defaultValue = "true") boolean testMode) {
        try {
            return ApiResponse.ok(service.processChuaPsgd(testMode));
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
