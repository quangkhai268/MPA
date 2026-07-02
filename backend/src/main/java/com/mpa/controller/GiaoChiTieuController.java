package com.mpa.controller;

import com.mpa.dto.BscSoSanhResponse;
import com.mpa.dto.ChiTieuBscRequest;
import com.mpa.dto.ChiTieuQuanLyRow;
import com.mpa.service.GiaoChiTieuService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/giao-chi-tieu")
@RequiredArgsConstructor
public class GiaoChiTieuController {

    private final GiaoChiTieuService service;

    @GetMapping("/so-sanh")
    public ApiResponse<BscSoSanhResponse> getSoSanh(
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(defaultValue = "") String selectedKy,
            @RequestParam(defaultValue = "phong") String doiTuong) {
        try {
            return ApiResponse.ok(service.getSoSanh(loaiKy, selectedKy, doiTuong));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải dữ liệu so sánh: " + e.getMessage());
        }
    }

    @GetMapping("/quan-ly")
    public ApiResponse<List<ChiTieuQuanLyRow>> getQuanLy(
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(defaultValue = "") String selectedKy,
            @RequestParam(defaultValue = "phong") String doiTuong) {
        try {
            return ApiResponse.ok(service.getQuanLyList(loaiKy, selectedKy, doiTuong));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách chỉ tiêu: " + e.getMessage());
        }
    }

    @PostMapping("/them")
    public ApiResponse<Void> themChiTieu(@RequestBody ChiTieuBscRequest request) {
        try {
            service.themChiTieu(request);
            return ApiResponse.ok(null);
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi thêm chỉ tiêu: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteChiTieu(@PathVariable Integer id) {
        try {
            service.deleteChiTieu(id);
            return ApiResponse.ok(null);
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi xóa chỉ tiêu: " + e.getMessage());
        }
    }

    @GetMapping("/phong-list")
    public ApiResponse<List<Map<String, String>>> getPhongList() {
        try {
            return ApiResponse.ok(service.getPhongList());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/cn-list")
    public ApiResponse<List<Map<String, String>>> getCnList() {
        try {
            return ApiResponse.ok(service.getCnList());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/am-list")
    public ApiResponse<List<Map<String, String>>> getAmList() {
        try {
            return ApiResponse.ok(service.getAmList());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
