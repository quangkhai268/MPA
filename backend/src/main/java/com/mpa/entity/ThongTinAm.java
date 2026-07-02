package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "thong_tin_am")
@Data
public class ThongTinAm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_cn")
    private String maCn;

    @Column(name = "ma_don_vi_cap_6")
    private String maDonViCap6;

    @Column(name = "ma_am")
    private String maAm;

    @Column(name = "ten_am")
    private String tenAm;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau;

    @Column(name = "email")
    private String email;

    @Column(name = "chuc_vu")
    private String chucVu;

    @Column(name = "loai_am")
    private String loaiAm;

    @Column(name = "trang_thai")
    private Short trangThai;
}
