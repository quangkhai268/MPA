package com.mpa.service;

import com.mpa.dto.DuLieuMpaPeriod;
import com.mpa.dto.FileImportResult;

import java.io.InputStream;
import java.util.List;

public interface DuLieuMpaImportService {

    /** Xóa sạch bảng staging — gọi 1 lần khi bắt đầu 1 phiên upload mới. */
    void clearStaging();

    /**
     * Đọc file Excel MPA (Tháng/Quý/Năm/Ngày, tự nhận diện qua header), ghi các dòng
     * hợp lệ vào bảng staging (chưa đụng vào bảng sống).
     */
    FileImportResult stageFile(InputStream excelStream, String fileName);

    /** Số dòng hiện đang có trong bảng staging (tổng dồn từ mọi file MPA trong phiên). */
    long countStaged();

    /**
     * Chỉ được gọi SAU KHI toàn bộ file MPA trong phiên đã stage xong không lỗi.
     * Trong 1 transaction: với mỗi loại kỳ (Tháng/Quý/Năm/Ngày) có mặt trong staging,
     * xóa đúng phạm vi dữ liệu cũ tương ứng trên bảng sống rồi nạp toàn bộ staging vào.
     *
     * @return danh sách các kỳ (loai_ky/nam/thang/quy/ngay) vừa được commit, dùng để
     *         đồng bộ tiếp sang thuc_hien_bsc_chi_nhanh/thuc_hien_bsc_khach_hang
     *         (xem BscSyncService) mà không cần quét lại toàn bảng du_lieu_mpa.
     */
    List<DuLieuMpaPeriod> commitStagedData();
}
