package com.mpa.controller;

import com.mpa.dto.ThongTinAmRequest;
import com.mpa.dto.ThongTinAmResponse;
import com.mpa.dto.ThongTinAmStatusCounts;
import com.mpa.service.ThongTinAmService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quan-ly-am")
@RequiredArgsConstructor
public class ThongTinAmController {

    private final ThongTinAmService service;

    @GetMapping
    public ApiResponse<Page<ThongTinAmResponse>> getList(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Short trangThai,
            @RequestParam(required = false) String maDonViCap6,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ApiResponse.ok(service.getList(search, trangThai, maDonViCap6, page, size));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ApiResponse<List<ThongTinAmResponse>> getAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Short trangThai,
            @RequestParam(required = false) String maDonViCap6) {
        try {
            return ApiResponse.ok(service.getAll(search, trangThai, maDonViCap6));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách: " + e.getMessage());
        }
    }

    @GetMapping("/status-counts")
    public ApiResponse<ThongTinAmStatusCounts> getStatusCounts(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String maDonViCap6) {
        try {
            return ApiResponse.ok(service.getStatusCounts(search, maDonViCap6));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi đếm số lượng AM: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<ThongTinAmResponse> create(@RequestBody ThongTinAmRequest request) {
        try {
            return ApiResponse.ok(service.create(request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi thêm cán bộ: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<ThongTinAmResponse> update(@PathVariable Integer id,
                                                   @RequestBody ThongTinAmRequest request) {
        try {
            return ApiResponse.ok(service.update(id, request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        try {
            service.delete(id);
            return ApiResponse.ok(null);
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi xóa: " + e.getMessage());
        }
    }

    @GetMapping("/am-dropdown")
    public ApiResponse<List<Map<String, String>>> getAmDropdown() {
        try {
            return ApiResponse.ok(service.getAmDropdown());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
