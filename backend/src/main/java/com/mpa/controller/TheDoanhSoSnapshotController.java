package com.mpa.controller;

import com.mpa.dto.RevenueSeriesResponse;
import com.mpa.service.SnapshotService;
import com.mpa.service.TheDoanhSoSnapshotService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/the-doanh-so")
@RequiredArgsConstructor
public class TheDoanhSoSnapshotController {

    private final TheDoanhSoSnapshotService service;
    private final SnapshotService snapshotService;

    @GetMapping("/card/{cardId}/series")
    public ApiResponse<RevenueSeriesResponse> getSeries(
            @PathVariable String cardId,
            @RequestParam(defaultValue = "ngay") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            LocalDate toDate = to != null ? to : LocalDate.now();
            LocalDate fromDate = from != null ? from : toDate.minusDays(90);
            return ApiResponse.ok(service.getSeries(cardId, granularity, fromDate, toDate));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải doanh số theo thời gian: " + e.getMessage());
        }
    }

    @GetMapping("/bao-cao")
    public ApiResponse<RevenueSeriesResponse> getBaoCaoTongHop(
            @RequestParam(defaultValue = "thang") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            LocalDate toDate = to != null ? to : LocalDate.now();
            LocalDate fromDate = from != null ? from : toDate.minusMonths(12);
            return ApiResponse.ok(service.getBaoCaoTongHop(granularity, fromDate, toDate));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải báo cáo doanh số tổng hợp: " + e.getMessage());
        }
    }

    @PostMapping("/snapshot/run-now")
    public ApiResponse<Integer> runNow() {
        try {
            return ApiResponse.ok(snapshotService.runDailySnapshot());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi chạy snapshot: " + e.getMessage());
        }
    }
}
