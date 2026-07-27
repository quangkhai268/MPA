package com.mpa.controller;

import com.mpa.dto.KhachHangChiTietResponse;
import com.mpa.dto.ThongTinKhachHangRequest;
import com.mpa.dto.ThongTinKhachHangResponse;
import com.mpa.service.ThongTinKhachHangService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class ThongTinKhachHangController {

    private final ThongTinKhachHangService service;

    @GetMapping
    public ApiResponse<Page<ThongTinKhachHangResponse>> getList(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String maDonViCap6,
            @RequestParam(defaultValue = "") String amSearch,
            @RequestParam(defaultValue = "") String phanKhuc,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ApiResponse.ok(service.getList(search, maDonViCap6, amSearch, phanKhuc, page, size));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách: " + e.getMessage());
        }
    }

    @GetMapping("/phan-khuc-list")
    public ApiResponse<List<String>> getPhanKhucList() {
        try {
            return ApiResponse.ok(service.getPhanKhucList());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<ThongTinKhachHangResponse> create(@RequestBody ThongTinKhachHangRequest request) {
        try {
            return ApiResponse.ok(service.create(request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi thêm khách hàng: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<ThongTinKhachHangResponse> update(@PathVariable Integer id,
                                                          @RequestBody ThongTinKhachHangRequest request) {
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

    @GetMapping("/{cif}/chi-tiet")
    public ApiResponse<KhachHangChiTietResponse> getChiTiet(
            @PathVariable String cif,
            @RequestParam int nam,
            @RequestParam(required = false) Integer thang,
            @RequestParam(required = false) String quy,
            @RequestParam(defaultValue = "false") boolean ngayMoiNhat,
            @RequestParam(defaultValue = "ky-truoc") String soVoi) {
        try {
            return ApiResponse.ok(service.getChiTiet(cif, nam, thang, quy, ngayMoiNhat, soVoi));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải chi tiết khách hàng: " + e.getMessage());
        }
    }
}
