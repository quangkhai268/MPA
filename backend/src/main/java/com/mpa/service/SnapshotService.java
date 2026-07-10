package com.mpa.service;

public interface SnapshotService {

    /**
     * Chụp lại doanh số lũy kế hiện tại của toàn bộ thẻ vào the_doanh_so_snapshot
     * cho ngày hôm nay. Idempotent — chạy nhiều lần trong cùng 1 ngày chỉ ghi đè
     * cùng 1 dòng, không tạo lịch sử giả.
     * @return số thẻ đã snapshot
     */
    int runDailySnapshot();
}
