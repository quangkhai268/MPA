package com.mpa.controller;

import com.mpa.dto.KhachHangTheSummaryResponse;
import com.mpa.dto.ThePhatHanhDetailResponse;
import com.mpa.dto.ThePhatHanhResponse;
import com.mpa.dto.TheSummaryResponse;
import com.mpa.entity.Role;
import com.mpa.entity.ThePhatHanh;
import com.mpa.security.CustomUserDetails;
import com.mpa.service.ThePhatHanhExportService;
import com.mpa.service.ThePhatHanhService;
import com.mpa.util.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/the-phat-hanh")
@RequiredArgsConstructor
public class ThePhatHanhController {

    private final ThePhatHanhService service;
    private final ThePhatHanhExportService exportService;

    // Giá trị không khớp bất kỳ ma_don_vi_cap_6 thật nào — dùng để ép "0 kết quả" cho user
    // không phải ADMIN nhưng chưa được gán phòng ban, thay vì lỡ tay cho null lọt qua thành
    // "không giới hạn" (ý nghĩa null bên dưới CHỈ dành cho ADMIN).
    private static final String NO_DON_VI_SENTINEL = "__NO_DON_VI__";

    /** User không phải ADMIN chỉ được xem thẻ thuộc phòng ban của chính mình — ép cứng
     *  maDonViCap6 theo phòng ban của user đăng nhập phía server, KHÔNG tin tham số client gửi
     *  lên (client có thể tự sửa query param để xem phòng khác nếu chỉ lọc ở frontend). */
    private String scopeMaDonViCap6(CustomUserDetails principal, String requested) {
        if (principal.getUser().getRole() == Role.ROLE_ADMIN) return requested;
        String own = principal.getUser().getMaDonViCap6();
        return own != null ? own : NO_DON_VI_SENTINEL;
    }

    @GetMapping
    public ApiResponse<Page<ThePhatHanhResponse>> getList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "")    String search,
            @RequestParam(defaultValue = "")    String trangThai,
            @RequestParam(defaultValue = "")    String hinhThuc,
            @RequestParam(defaultValue = "")    String productCode,
            @RequestParam(defaultValue = "")    String loaiTheTinDung,
            @RequestParam(required = false)     String maDonViCap6,
            @RequestParam(defaultValue = "")    String amSearch,
            @RequestParam(required = false)     List<String> amCodes,
            @RequestParam(defaultValue = "false") boolean chuaKichHoat,
            @RequestParam(defaultValue = "0")   int soNgayMin,
            @RequestParam(defaultValue = "false") boolean chuaPsgd,
            @RequestParam(defaultValue = "false") boolean chuaDatPtn,
            @RequestParam(defaultValue = "false") boolean datPtn,
            @RequestParam(defaultValue = "0")   int soNgayThuPtn,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size) {
        try {
            return ApiResponse.ok(service.getList(search, trangThai, hinhThuc, productCode,
                    loaiTheTinDung, scopeMaDonViCap6(principal, maDonViCap6), amSearch, amCodes, chuaKichHoat, soNgayMin, chuaPsgd, chuaDatPtn, datPtn, soNgayThuPtn, page, size));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi tải danh sách thẻ: " + e.getMessage());
        }
    }

    /** Xuất Excel toàn bộ thẻ khớp filter hiện tại (không phân trang). */
    @GetMapping("/export")
    public void export(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "")    String search,
            @RequestParam(defaultValue = "")    String trangThai,
            @RequestParam(defaultValue = "")    String hinhThuc,
            @RequestParam(defaultValue = "")    String productCode,
            @RequestParam(defaultValue = "")    String loaiTheTinDung,
            @RequestParam(required = false)     String maDonViCap6,
            @RequestParam(defaultValue = "")    String amSearch,
            @RequestParam(required = false)     List<String> amCodes,
            @RequestParam(defaultValue = "false") boolean chuaKichHoat,
            @RequestParam(defaultValue = "0")   int soNgayMin,
            @RequestParam(defaultValue = "false") boolean chuaPsgd,
            @RequestParam(defaultValue = "false") boolean chuaDatPtn,
            @RequestParam(defaultValue = "false") boolean datPtn,
            @RequestParam(defaultValue = "0")   int soNgayThuPtn,
            HttpServletResponse response) throws IOException {
        List<ThePhatHanh> cards = service.exportList(search, trangThai, hinhThuc, productCode,
                loaiTheTinDung, scopeMaDonViCap6(principal, maDonViCap6), amSearch, amCodes, chuaKichHoat, soNgayMin, chuaPsgd, chuaDatPtn, datPtn, soNgayThuPtn);
        byte[] bytes = exportService.exportExcel(cards);

        String fileName = "danh-sach-the-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''"
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    @GetMapping("/{id}")
    public ApiResponse<ThePhatHanhDetailResponse> getDetail(@AuthenticationPrincipal CustomUserDetails principal,
                                                             @PathVariable Long id) {
        try {
            return ApiResponse.ok(service.getDetail(id, scopeMaDonViCap6(principal, null)));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi tải chi tiết thẻ: " + e.getMessage());
        }
    }

    @GetMapping("/summary")
    public ApiResponse<TheSummaryResponse> getSummary(@AuthenticationPrincipal CustomUserDetails principal) {
        try {
            return ApiResponse.ok(service.getSummary(scopeMaDonViCap6(principal, null)));
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
