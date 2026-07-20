package com.mpa.service;

import com.mpa.dto.KhachHangTheSummaryResponse;
import com.mpa.dto.ThePhatHanhDetailResponse;
import com.mpa.dto.ThePhatHanhResponse;
import com.mpa.dto.TheSummaryResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ThePhatHanhService {

    Page<ThePhatHanhResponse> getList(
            String search, String trangThai, String hinhThuc, String productCode,
            String loaiTheTinDung,
            boolean chuaKichHoat, int soNgayMin, boolean chuaPsgd, boolean chuaDatPtn, boolean datPtn,
            int page, int size);

    ThePhatHanhDetailResponse getDetail(Long id);

    TheSummaryResponse getSummary();

    /** Tổng hợp thẻ tín dụng của 1 khách hàng — join thong_tin_khach_hang.ma_kh_cif = the_phat_hanh.so_cif_khach_hang_pht. */
    KhachHangTheSummaryResponse getSummaryByCif(String cif);

    List<String> getDistinctTrangThai();

    List<String> getDistinctHinhThuc();

    List<String> getDistinctProductCode();
}
