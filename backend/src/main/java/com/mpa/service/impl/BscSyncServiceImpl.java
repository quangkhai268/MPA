package com.mpa.service.impl;

import com.mpa.dto.DuLieuMpaPeriod;
import com.mpa.service.BscSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Đồng bộ 1 kỳ vừa import vào du_lieu_mpa sang thuc_hien_bsc_chi_nhanh (tổng chi nhánh +
 * phòng + AM) và thuc_hien_bsc_khach_hang. Thay thế cho việc chạy tay các câu lệnh SQL
 * INSERT thuần trước đây — 2 vấn đề đã sửa: (1) luôn lọc theo loai_ky ở nguồn (tránh lẫn
 * dữ liệu Tháng/Quý/Năm/Ngày), (2) DELETE đúng phạm vi trước khi INSERT (idempotent —
 * gọi lại nhiều lần cho cùng 1 kỳ không cộng dồn).
 *
 * Ánh xạ loai_ky (du_lieu_mpa) → type_data (BSC): THANG→1, QUY→5, NAM→6, NGAY→1 (khoá theo
 * ngay). type_data=2 (phòng) và 3 (AM) dùng CHUNG cho mọi kỳ (giống quy ước có sẵn trong
 * ThucHienBscRepository), phân biệt bằng cột nào được điền — vì vậy DELETE của nhánh NAM
 * (chỉ set nam, không set thang/quy) phải thêm điều kiện "thang IS NULL AND quy IS NULL"
 * để không xoá nhầm dữ liệu Tháng/Quý cùng năm đó (2 nhánh kia tự loại trừ nhau qua so
 * khớp bằng cột riêng — không match giá trị NULL của grain khác).
 *
 * Riêng NAM: bảng BSC không có khái niệm "nhiều mốc lũy kế" như du_lieu_mpa — mỗi lần
 * đồng bộ ghi đè type_data=6 của đúng năm đó bằng số liệu của mốc mới nhất, không giữ
 * lịch sử nhiều mốc (xem taidulieu.md / plan mục "Bổ sung 1").
 */
@Service
@RequiredArgsConstructor
public class BscSyncServiceImpl implements BscSyncService {

    private final JdbcTemplate jdbcTemplate;

    private static final String CHI_NHANH_SUBQUERY =
            "(SELECT ma_cn FROM chi_nhanh LIMIT 1), (SELECT ten_cn FROM chi_nhanh LIMIT 1)";

    private static final String SUM_FRAGMENT =
            "COALESCE(SUM(thu_nhap_thuan_hdv_ftp),0), COALESCE(SUM(thu_nhap_thuan_dich_vu),0), " +
            "COALESCE(SUM(thu_nhap_thuan_tin_dung),0), COALESCE(SUM(thu_nhap_thuan),0), " +
            "COALESCE(SUM(du_no_tin_dung_cuoi_ky),0), COALESCE(SUM(huy_dong_von_binh_quan),0), " +
            "COALESCE(SUM(huy_dong_von_cuoi_ky),0), " +
            "COALESCE(SUM(CASE WHEN ky_han_cap_2 = 'Khong ky han' THEN huy_dong_von_binh_quan ELSE 0 END),0)";

    private static final String METRIC_COLUMNS =
            "thu_nhap_thuan_hdv_ftp, thu_nhap_thuan_dich_vu, thu_nhap_thuan_tin_dung, thu_nhap_thuan, " +
            "du_no_tin_dung_cuoi_ky, huy_dong_von_binh_quan, huy_dong_von_cuoi_ky, casa_binh_quan";

    @Override
    @Transactional
    public void syncPeriod(DuLieuMpaPeriod period) {
        switch (period.loaiKy()) {
            case "THANG" -> syncThang(period.nam(), period.thang());
            case "QUY" -> syncQuy(period.nam(), period.quy());
            case "NAM" -> syncNam(period.nam(), period.thang());
            case "NGAY" -> syncNgay(period.ngay());
            default -> throw new IllegalArgumentException("loai_ky không hợp lệ: " + period.loaiKy());
        }
    }

    private void syncThang(int nam, int thang) {
        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 1 AND nam = ? AND thang = ?", nam, thang);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (thang, nam, ma_cn, ten_cn, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", " + SUM_FRAGMENT + ", 1 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'THANG' AND nam = ? AND thang = ?",
                thang, nam, nam, thang);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 2 AND nam = ? AND thang = ?", nam, thang);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (thang, nam, ma_cn, ten_cn, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", ma_don_vi_cap_6, MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " +
                SUM_FRAGMENT + ", 2 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'THANG' AND nam = ? AND thang = ? AND ma_don_vi_cap_6 IS NOT NULL " +
                "GROUP BY ma_don_vi_cap_6",
                thang, nam, nam, thang);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 3 AND nam = ? AND thang = ?", nam, thang);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (thang, nam, ma_cn, ten_cn, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                "ma_am, ten_am, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", MAX(ma_don_vi_cap_6), MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " +
                "ma_am, MAX(ten_am), " + SUM_FRAGMENT + ", 3 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'THANG' AND nam = ? AND thang = ? AND ma_am IS NOT NULL " +
                "GROUP BY ma_am",
                thang, nam, nam, thang);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_khach_hang WHERE type_data = 1 AND nam = ? AND thang = ?", nam, thang);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_khach_hang (thang, nam, ma_cn, ten_cn, ma_kh_cif, ten_khach_hang, ma_am, ten_am, " +
                "ma_don_vi_cap_6, ten_don_vi_cap_6, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", ma_kh_cif, MAX(ten_khach_hang), ma_am, MAX(ten_am), " +
                "MAX(ma_don_vi_cap_6), MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " + SUM_FRAGMENT + ", 1 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'THANG' AND nam = ? AND thang = ? " +
                "GROUP BY ma_kh_cif, ma_am",
                thang, nam, nam, thang);
    }

    private void syncQuy(int nam, String quy) {
        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 5 AND nam = ? AND quy = ?", nam, quy);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (quy, nam, ma_cn, ten_cn, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", " + SUM_FRAGMENT + ", 5 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'QUY' AND nam = ? AND quy = ?",
                quy, nam, nam, quy);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 2 AND nam = ? AND quy = ?", nam, quy);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (quy, nam, ma_cn, ten_cn, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", ma_don_vi_cap_6, MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " +
                SUM_FRAGMENT + ", 2 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'QUY' AND nam = ? AND quy = ? AND ma_don_vi_cap_6 IS NOT NULL " +
                "GROUP BY ma_don_vi_cap_6",
                quy, nam, nam, quy);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 3 AND nam = ? AND quy = ?", nam, quy);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (quy, nam, ma_cn, ten_cn, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                "ma_am, ten_am, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", MAX(ma_don_vi_cap_6), MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " +
                "ma_am, MAX(ten_am), " + SUM_FRAGMENT + ", 3 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'QUY' AND nam = ? AND quy = ? AND ma_am IS NOT NULL " +
                "GROUP BY ma_am",
                quy, nam, nam, quy);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_khach_hang WHERE type_data = 5 AND nam = ? AND quy = ?", nam, quy);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_khach_hang (quy, nam, ma_cn, ten_cn, ma_kh_cif, ten_khach_hang, ma_am, ten_am, " +
                "ma_don_vi_cap_6, ten_don_vi_cap_6, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", ma_kh_cif, MAX(ten_khach_hang), ma_am, MAX(ten_am), " +
                "MAX(ma_don_vi_cap_6), MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " + SUM_FRAGMENT + ", 5 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'QUY' AND nam = ? AND quy = ? " +
                "GROUP BY ma_kh_cif, ma_am",
                quy, nam, nam, quy);
    }

    /**
     * Nguồn lọc theo (nam, thang=mốc lũy kế vừa import); đích chỉ khoá theo nam
     * (không mang mốc sang BSC — luôn ghi đè bằng số liệu của mốc mới nhất).
     */
    private void syncNam(int nam, Integer mocThang) {
        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 6 AND nam = ?", nam);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (nam, ma_cn, ten_cn, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, " + CHI_NHANH_SUBQUERY + ", " + SUM_FRAGMENT + ", 6 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'NAM' AND nam = ? AND thang = ?",
                nam, nam, mocThang);

        // type_data=2/3 dùng chung cho mọi kỳ — phải loại trừ rõ thang/quy để không xoá
        // nhầm dữ liệu Tháng/Quý cùng năm (nhánh này chỉ set nam, không set thang/quy).
        jdbcTemplate.update(
                "DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 2 AND nam = ? AND thang IS NULL AND quy IS NULL", nam);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (nam, ma_cn, ten_cn, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, " + CHI_NHANH_SUBQUERY + ", ma_don_vi_cap_6, MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " +
                SUM_FRAGMENT + ", 2 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'NAM' AND nam = ? AND thang = ? AND ma_don_vi_cap_6 IS NOT NULL " +
                "GROUP BY ma_don_vi_cap_6",
                nam, nam, mocThang);

        jdbcTemplate.update(
                "DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 3 AND nam = ? AND thang IS NULL AND quy IS NULL", nam);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (nam, ma_cn, ten_cn, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                "ma_am, ten_am, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, " + CHI_NHANH_SUBQUERY + ", MAX(ma_don_vi_cap_6), MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " +
                "ma_am, MAX(ten_am), " + SUM_FRAGMENT + ", 3 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'NAM' AND nam = ? AND thang = ? AND ma_am IS NOT NULL " +
                "GROUP BY ma_am",
                nam, nam, mocThang);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_khach_hang WHERE type_data = 6 AND nam = ?", nam);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_khach_hang (nam, ma_cn, ten_cn, ma_kh_cif, ten_khach_hang, ma_am, ten_am, " +
                "ma_don_vi_cap_6, ten_don_vi_cap_6, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, " + CHI_NHANH_SUBQUERY + ", ma_kh_cif, MAX(ten_khach_hang), ma_am, MAX(ten_am), " +
                "MAX(ma_don_vi_cap_6), MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " + SUM_FRAGMENT + ", 6 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'NAM' AND nam = ? AND thang = ? " +
                "GROUP BY ma_kh_cif, ma_am",
                nam, nam, mocThang);
    }

    /** Khoá theo ngay (giống hệt cách sumThucHienByNgay đọc). Không đồng bộ thuc_hien_bsc_khach_hang. */
    private void syncNgay(LocalDate ngay) {
        int nam = ngay.getYear();
        Date sqlNgay = Date.valueOf(ngay);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 1 AND ngay = ?", sqlNgay);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (ngay, nam, ma_cn, ten_cn, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", " + SUM_FRAGMENT + ", 1 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'NGAY' AND ngay = ?",
                sqlNgay, nam, sqlNgay);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 2 AND ngay = ?", sqlNgay);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (ngay, nam, ma_cn, ten_cn, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", ma_don_vi_cap_6, MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " +
                SUM_FRAGMENT + ", 2 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'NGAY' AND ngay = ? AND ma_don_vi_cap_6 IS NOT NULL " +
                "GROUP BY ma_don_vi_cap_6",
                sqlNgay, nam, sqlNgay);

        jdbcTemplate.update("DELETE FROM thuc_hien_bsc_chi_nhanh WHERE type_data = 3 AND ngay = ?", sqlNgay);
        jdbcTemplate.update(
                "INSERT INTO thuc_hien_bsc_chi_nhanh (ngay, nam, ma_cn, ten_cn, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                "ma_am, ten_am, " + METRIC_COLUMNS + ", type_data) " +
                "SELECT ?, ?, " + CHI_NHANH_SUBQUERY + ", MAX(ma_don_vi_cap_6), MAX(replace(ten_don_vi_cap_6,'_CN TAY HO','')), " +
                "ma_am, MAX(ten_am), " + SUM_FRAGMENT + ", 3 " +
                "FROM du_lieu_mpa WHERE loai_ky = 'NGAY' AND ngay = ? AND ma_am IS NOT NULL " +
                "GROUP BY ma_am",
                sqlNgay, nam, sqlNgay);
    }
}
