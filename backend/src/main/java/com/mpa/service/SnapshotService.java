package com.mpa.service;

import java.time.LocalDate;

public interface SnapshotService {

    /**
     * Chụp lại doanh số lũy kế hiện tại của toàn bộ thẻ (the_phat_hanh) vào
     * the_doanh_so_snapshot cho đúng ngày truyền vào. Idempotent — chạy nhiều lần
     * cho cùng 1 ngày (VD nhiều lần import ISS_02 trong ngày) chỉ ghi đè cùng 1 dòng
     * /thẻ/ngày với giá trị mới nhất, không tạo lịch sử giả.
     * @return số thẻ đã snapshot
     */
    int runSnapshot(LocalDate ngaySnapshot);
}
