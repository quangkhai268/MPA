package com.mpa.controller;

import com.mpa.dto.ThePhatHanhResponse;
import com.mpa.dto.TheSummaryResponse;
import com.mpa.service.ThePhatHanhService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/the-phat-hanh")
@RequiredArgsConstructor
public class ThePhatHanhController {

    private final ThePhatHanhService service;

    @GetMapping
    public ApiResponse<Page<ThePhatHanhResponse>> getList(
            @RequestParam(defaultValue = "")    String search,
            @RequestParam(defaultValue = "")    String trangThai,
            @RequestParam(defaultValue = "")    String hinhThuc,
            @RequestParam(defaultValue = "")    String productCode,
            @RequestParam(defaultValue = "")    String loaiTheTinDung,
            @RequestParam(defaultValue = "false") boolean chuaKichHoat,
            @RequestParam(defaultValue = "false") boolean chuaPsgd,
            @RequestParam(defaultValue = "false") boolean chuaDatPtn,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size) {
        try {
            return ApiResponse.ok(service.getList(search, trangThai, hinhThuc, productCode,
                    loaiTheTinDung, chuaKichHoat, chuaPsgd, chuaDatPtn, page, size));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi tải danh sách thẻ: " + e.getMessage());
        }
    }

    @GetMapping("/summary")
    public ApiResponse<TheSummaryResponse> getSummary() {
        try {
            return ApiResponse.ok(service.getSummary());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi tải tổng quan thẻ: " + e.getMessage());
        }
    }

    @GetMapping("/trang-thai-options")
    public ApiResponse<List<String>> getTrangThaiOptions() {
        try {
            return ApiResponse.ok(service.getDistinctTrangThai());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/hinh-thuc-options")
    public ApiResponse<List<String>> getHinhThucOptions() {
        try {
            return ApiResponse.ok(service.getDistinctHinhThuc());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/product-options")
    public ApiResponse<List<String>> getProductOptions() {
        try {
            return ApiResponse.ok(service.getDistinctProductCode());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
