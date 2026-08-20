package com.mpa.service;

import com.mpa.dto.KhachHangTheSummaryResponse;
import com.mpa.dto.ThePhatHanhDetailResponse;
import com.mpa.dto.ThePhatHanhResponse;
import com.mpa.dto.TheSummaryResponse;
import com.mpa.entity.ThePhatHanh;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ThePhatHanhService {

    Page<ThePhatHanhResponse> getList(
            String search, String trangThai, String hinhThuc, String productCode,
            String loaiTheTinDung, String maDonViCap6, String amSearch, List<String> amCodes,
            boolean chuaKichHoat, int soNgayMin, boolean chuaPsgd, boolean chuaDatPtn, boolean datPtn,
            int soNgayThuPtn, int page, int size);

    /** Toàn bộ thẻ khớp filter (không phân trang) — dùng cho Xuất Excel. */
    List<ThePhatHanh> exportList(
            String search, String trangThai, String hinhThuc, String productCode,
            String loaiTheTinDung, String maDonViCap6, String amSearch, List<String> amCodes,
            boolean chuaKichHoat, int soNgayMin, boolean chuaPsgd, boolean chuaDatPtn, boolean datPtn,
            int soNgayThuPtn);

    ThePhatHanhDetailResponse getDetail(Long id);

    TheSummaryResponse getSummary();

    /** Tổng hợp thẻ tín dụng của 1 khách hàng — join thong_tin_khach_hang.ma_kh_cif = the_phat_hanh.so_cif_khach_hang_pht. */
    KhachHangTheSummaryResponse getSummaryByCif(String cif);

    List<String> getDistinctTrangThai();

    List<String> getDistinctHinhThuc();

    List<String> getDistinctProductCode();
}
