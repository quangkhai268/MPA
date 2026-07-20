package com.mpa.service;

import com.mpa.dto.FileImportResult;

import java.io.InputStream;

public interface ThePhatHanhImportService {

    /** Xóa sạch bảng staging — gọi 1 lần khi bắt đầu 1 phiên upload mới. */
    void clearStaging();

    /** Đọc file Excel ISS_02, ghi các dòng hợp lệ vào bảng staging (chưa đụng vào bảng sống). */
    FileImportResult stageFile(InputStream excelStream, String fileName);

    /** Số dòng hiện đang có trong bảng staging (tổng dồn từ mọi file ISS_02 trong phiên). */
    long countStaged();

    /**
     * Chỉ được gọi SAU KHI toàn bộ file trong phiên đã stage xong không lỗi.
     * Trong 1 transaction: xóa dữ liệu the_phat_hanh cũ rồi nạp toàn bộ dữ liệu từ staging vào.
     */
    void commitStagedData();
}
