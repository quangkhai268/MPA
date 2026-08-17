package com.mpa.service;

import com.mpa.dto.FileImportResult;

import java.io.InputStream;

public interface ThongTinAmImportService {

    /** Nhận diện file "thông tin AM" bằng ô tiêu đề đầu tiên = "Mã định danh" (không dựa vào tên file). */
    boolean isThongTinAmFile(byte[] excelBytes);

    /** Đọc file Excel và upsert thẳng vào bảng thong_tin_am theo mã định danh (ma_am) — không qua staging. */
    FileImportResult importFile(InputStream excelStream, String fileName);
}
