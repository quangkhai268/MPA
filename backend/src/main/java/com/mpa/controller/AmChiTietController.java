package com.mpa.controller;

import com.mpa.dto.AmChiTietResponse;
import com.mpa.dto.XuHuongResponse;
import com.mpa.entity.ThongTinAm;
import com.mpa.repository.ThongTinAmRepository;
import com.mpa.service.AmChiTietService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Chi tiết theo mã AM (`quan-ly-am/:maAm`) hoặc theo cán bộ — gộp toàn bộ mã AM cùng tên
 * (`quan-ly-am/can-bo/:tenAm`). Truyền đúng 1 trong 2 tham số `maAm` / `tenAm`.
 */
@RestController
@RequestMapping("/api/quan-ly-am")
@RequiredArgsConstructor
public class AmChiTietController {

    private final AmChiTietService service;
    private final ThongTinAmRepository thongTinAmRepo;

    @GetMapping("/chi-tiet")
    public ApiResponse<AmChiTietResponse> getChiTiet(
            @RequestParam(required = false) String maAm,
            @RequestParam(required = false) String tenAm,
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam(defaultValue = "") String selectedKy) {
        try {
            Scope scope = resolveScope(maAm, tenAm);
            return ApiResponse.ok(service.getChiTiet(scope.entities(), scope.isCanBo(), loaiKy, selectedKy));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải chi tiết AM: " + e.getMessage());
        }
    }

    @GetMapping("/xu-huong")
    public ApiResponse<XuHuongResponse> getXuHuong(
            @RequestParam(required = false) String maAm,
            @RequestParam(required = false) String tenAm,
            @RequestParam(defaultValue = "thang") String loaiKy,
            @RequestParam int nam) {
        try {
            Scope scope = resolveScope(maAm, tenAm);
            return ApiResponse.ok(service.getXuHuong(scope.entities(), loaiKy, nam));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải dữ liệu xu hướng AM: " + e.getMessage());
        }
    }

    private record Scope(List<ThongTinAm> entities, boolean isCanBo) {}

    private Scope resolveScope(String maAm, String tenAm) {
        if (maAm != null && !maAm.isBlank()) {
            ThongTinAm entity = thongTinAmRepo.findByMaAm(maAm.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã AM: " + maAm));
            return new Scope(List.of(entity), false);
        }
        if (tenAm != null && !tenAm.isBlank()) {
            List<ThongTinAm> entities = thongTinAmRepo.findByTenAm(tenAm.trim());
            if (entities.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy cán bộ: " + tenAm);
            }
            return new Scope(entities, true);
        }
        throw new IllegalArgumentException("Thiếu tham số maAm hoặc tenAm");
    }
}
