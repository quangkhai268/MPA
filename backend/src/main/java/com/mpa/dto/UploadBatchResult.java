package com.mpa.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UploadBatchResult {
    private LocalDateTime thoiGian;
    private String nguoiUpload;
    private LocalDate ngayDuLieu;
    private int soFile;
    private int tongDong;
    private String trangThai;   // SUCCESS | PARTIAL | FAILED | UNSUPPORTED
    private List<FileImportResult> files;
}
