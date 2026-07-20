package com.mpa.controller;

import com.mpa.dto.KhachHangTheSummaryResponse;
import com.mpa.dto.ThePhatHanhDetailResponse;
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
            @RequestParam(defaultValue = "0")   int soNgayMin,
            @RequestParam(defaultValue = "false") boolean chuaPsgd,
            @RequestParam(defaultValue = "false") boolean chuaDatPtn,
            @RequestParam(defaultValue = "false") boolean datPtn,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size) {
        try {
            return ApiResponse.ok(service.getList(search, trangThai, hinhThuc, productCode,
                    loaiTheTinDung, chuaKichHoat, soNgayMin, chuaPsgd, chuaDatPtn, datPtn, page, size));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi tải danh sách thẻ: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<ThePhatHanhDetailResponse> getDetail(@PathVariable Long id) {
        try {
            return ApiResponse.ok(service.getDetail(id));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi tải chi tiết thẻ: " + e.getMessage());
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

    /** Tổng hợp thẻ tín dụng của 1 khách hàng — dùng ở trang khach-hang-detail. */
    @GetMapping("/theo-khach-hang/{cif}")
    public ApiResponse<KhachHangTheSummaryResponse> getSummaryByCif(@PathVariable String cif) {
        try {
            return ApiResponse.ok(service.getSummaryByCif(cif));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi tải danh sách thẻ của khách hàng: " + e.getMessage());
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
