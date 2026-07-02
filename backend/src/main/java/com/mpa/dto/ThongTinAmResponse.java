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
public class ThongTinAmResponse {
    private Integer id;
    private String maCn;
    private String maDonViCap6;
    private String tenPhong;
    private String tenCn;
    private String maAm;
    private String tenAm;
    private String soDienThoai;
    private LocalDate ngayBatDau;
    private String email;
    private String chucVu;
    private String loaiAm;
    private Short trangThai;
}
