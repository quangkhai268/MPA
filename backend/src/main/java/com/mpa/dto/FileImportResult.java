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
}
