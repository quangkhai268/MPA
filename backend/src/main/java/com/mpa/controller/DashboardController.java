package com.mpa.controller;

import com.mpa.dto.AmDetailResponse;
import com.mpa.dto.DashboardKpiResponse;
import com.mpa.dto.PhongDataResponse;
import com.mpa.dto.PhongTrendSeriesResponse;
import com.mpa.dto.TrendDataResponse;
import com.mpa.service.DashboardService;
import com.mpa.util.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpi")
    public ApiResponse<DashboardKpiResponse> getKpi(
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(defaultValue = "") String selectedKy,
            @RequestParam(defaultValue = "ky-truoc") String soVoi) {
        try {
            return ApiResponse.ok(dashboardService.getKpi(loaiKy, selectedKy, soVoi));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải KPI: " + e.getMessage());
        }
    }

    @GetMapping("/trend")
    public ApiResponse<TrendDataResponse> getTrend(
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(defaultValue = "") String selectedKy,
            @RequestParam(defaultValue = "tong-tnt") String metricKey) {
        try {
            return ApiResponse.ok(dashboardService.getTrend(loaiKy, selectedKy, metricKey));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải xu hướng: " + e.getMessage());
        }
    }

    @GetMapping("/phong-trend")
    public ApiResponse<List<PhongTrendSeriesResponse>> getPhongTrend(
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(defaultValue = "") String selectedKy,
            @RequestParam(defaultValue = "tong-tnt") String metricKey) {
        try {
            return ApiResponse.ok(dashboardService.getPhongTrend(loaiKy, selectedKy, metricKey));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải xu hướng phòng: " + e.getMessage());
        }
    }

    @GetMapping("/by-phong")
    public ApiResponse<List<PhongDataResponse>> getByPhong(
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(defaultValue = "") String selectedKy,
            @RequestParam(defaultValue = "ky-truoc") String soVoi) {
        try {
            return ApiResponse.ok(dashboardService.getPhongData(loaiKy, selectedKy, soVoi));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải dữ liệu phòng: " + e.getMessage());
        }
    }

    @GetMapping("/am-detail")
    public ApiResponse<List<AmDetailResponse>> getAmDetail(
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(defaultValue = "") String selectedKy,
            @RequestParam(defaultValue = "") String maDonViCap6,
            @RequestParam(defaultValue = "ky-truoc") String soVoi) {
        try {
            return ApiResponse.ok(dashboardService.getAmDetail(loaiKy, selectedKy, maDonViCap6, soVoi));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải chi tiết AM: " + e.getMessage());
        }
    }
}
