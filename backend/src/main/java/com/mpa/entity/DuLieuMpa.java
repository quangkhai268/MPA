package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "du_lieu_mpa")
@Data
public class DuLieuMpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ngay")
    private LocalDate ngay;

    @Column(name = "thang")
    private Integer thang;

    @Column(name = "quy")
    private String quy;

    @Column(name = "nam")
    private Integer nam;

    @Column(name = "ma_am")
    private String maAm;

    @Column(name = "ten_am")
    private String tenAm;

    @Column(name = "ma_don_vi_cap_6")
    private String maDonViCap6;

    @Column(name = "ten_don_vi_cap_6")
    private String tenDonViCap6;

    @Column(name = "ma_sp_cap_5")
    private String maSpCap5;

    @Column(name = "ten_sp_cap_5")
    private String tenSpCap5;

    @Column(name = "ma_phan_khuc_kh_cap_2")
    private String maPhanKhucKhCap2;

    @Column(name = "ten_phan_khuc_kh_cap_2")
    private String tenPhanKhucKhCap2;

    @Column(name = "ma_kh_cif")
    private String maKhCif;

    @Column(name = "ten_khach_hang")
    private String tenKhachHang;

    @Column(name = "ky_han_cap_2")
    private String kyHanCap2;

    @Column(name = "thu_nhap_thuan_hdv_ftp")
    private BigDecimal thuNhapThuanHdvFtp;

    @Column(name = "thu_nhap_thuan_dich_vu")
    private BigDecimal thuNhapThuanDichVu;

    @Column(name = "thu_nhap_thuan_tin_dung")
    private BigDecimal thuNhapThuanTinDung;

    @Column(name = "thu_nhap_thuan")
    private BigDecimal thuNhapThuan;

    @Column(name = "du_no_tin_dung_cuoi_ky")
    private BigDecimal duNoTinDungCuoiKy;

    @Column(name = "huy_dong_von_binh_quan")
    private BigDecimal huyDongVonBinhQuan;

    @Column(name = "huy_dong_von_cuoi_ky")
    private BigDecimal huyDongVonCuoiKy;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "sheetname")
    private String sheetname;
}
