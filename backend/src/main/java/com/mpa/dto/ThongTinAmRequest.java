package com.mpa.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ThongTinAmRequest {
    private String maCn;
    private String maDonViCap6;
    private String maAm;
    private String tenAm;
    private String soDienThoai;
    private LocalDate ngayBatDau;
    private String email;
    private String chucVu;
    private String loaiAm;
    private Short trangThai;
}
