package com.mpa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThongTinKhachHangResponse {
    private Integer id;
    private String maCn;
    private String maDonViCap6;
    private String maAm;
    private String tenAm;
    private String tenPhong;
    private String maKhCif;
    private String tenKhachHang;
    private String soDienThoai;
    private LocalDate ngayBatDau;
    private String email;
    private Integer typeKhachHang;
    private String tenPhanKhuc;
    private Short trangThai;
}
