package com.mpa.dto;

import java.time.LocalDate;

/**
 * 1 kỳ dữ liệu vừa được commit vào du_lieu_mpa (1 tổ hợp loai_ky/nam/thang/quy/ngay).
 * Dùng để scope chính xác bước đồng bộ sang thuc_hien_bsc_chi_nhanh/thuc_hien_bsc_khach_hang
 * ngay sau khi import — không cần quét lại toàn bảng.
 */
public record DuLieuMpaPeriod(String loaiKy, Integer nam, Integer thang, String quy, LocalDate ngay) {
}
