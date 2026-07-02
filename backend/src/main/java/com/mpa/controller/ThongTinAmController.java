package com.mpa.controller;

import com.mpa.dto.ThongTinAmRequest;
import com.mpa.dto.ThongTinAmResponse;
import com.mpa.service.ThongTinAmService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ApiResponse.ok(service.getList(search, trangThai, page, size));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ApiResponse<List<ThongTinAmResponse>> getAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Short trangThai) {
        try {
            return ApiResponse.ok(service.getAll(search, trangThai));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách: " + e.getMessage());
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
    public ApiResponse<ThongTinAmResponse> update(@PathVariable Integer id,
                                                   @RequestBody ThongTinAmRequest request) {
        try {
            return ApiResponse.ok(service.update(id, request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
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
