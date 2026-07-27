package com.mpa.service;

import com.mpa.dto.DuLieuMpaPeriod;

public interface BscSyncService {

    /**
     * Đồng bộ 1 kỳ vừa import vào du_lieu_mpa sang thuc_hien_bsc_chi_nhanh
     * (tổng chi nhánh + phòng + AM) và thuc_hien_bsc_khach_hang (trừ loại NGAY).
     * Idempotent — gọi lại nhiều lần cho cùng 1 kỳ luôn cho ra đúng 1 bộ số liệu
     * (DELETE đúng phạm vi rồi INSERT lại, không cộng dồn).
     */
    void syncPeriod(DuLieuMpaPeriod period);
}
