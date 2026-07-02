package com.mpa.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ThongTinKhachHangRequest {
    private String maCn;
    private String maDonViCap6;
    private String maAm;
    private String maKhCif;
    private String tenKhachHang;
    private String soDienThoai;
    private LocalDate ngayBatDau;
    private String email;
    private Integer typeKhachHang;
    private Short trangThai;
}
