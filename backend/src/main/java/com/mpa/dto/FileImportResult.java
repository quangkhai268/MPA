package com.mpa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileImportResult {
    private String tenFile;
    private String loaiFile;   // MPA | ISS_02 | ISS_06 | ISS_15 | UNKNOWN
    private int soDong;
    private String trangThai;  // SUCCESS | FAILED | UNSUPPORTED
    private String ghiChu;
    private String kyLabel;    // Nhãn kỳ thân thiện, VD "Tháng 5/2026", "Quý 1/2026" — null nếu không xác định được (VD ISS_02)
}
