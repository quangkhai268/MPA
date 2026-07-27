package com.mpa.controller;

import com.mpa.dto.DuLieuMpaSummary;
import com.mpa.entity.DuLieuMpa;
import com.mpa.service.DuLieuMpaQueryService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mpa")
@RequiredArgsConstructor
public class DuLieuMpaController {

    private final DuLieuMpaQueryService service;

    @GetMapping
    public ApiResponse<Page<DuLieuMpa>> getList(
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(required = false) Integer thang,
            @RequestParam(required = false) String quy,
            @RequestParam(required = false) Integer nam,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) String maDonViCap6,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ApiResponse.ok(service.getList(loaiKy, thang, quy, nam, ngay, maDonViCap6, search, page, size));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải dữ liệu MPA: " + e.getMessage());
        }
    }

    @GetMapping("/summary")
    public ApiResponse<DuLieuMpaSummary> getSummary(
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(required = false) Integer thang,
            @RequestParam(required = false) String quy,
            @RequestParam(required = false) Integer nam,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestParam(required = false) String maDonViCap6,
            @RequestParam(defaultValue = "") String search) {
        try {
            return ApiResponse.ok(service.getSummary(loaiKy, thang, quy, nam, ngay, maDonViCap6, search));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải tổng hợp dữ liệu MPA: " + e.getMessage());
        }
    }

    @GetMapping("/phong-list")
    public ApiResponse<List<Map<String, String>>> getPhongList() {
        try {
            return ApiResponse.ok(service.getPhongList());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách đơn vị: " + e.getMessage());
        }
    }
}
