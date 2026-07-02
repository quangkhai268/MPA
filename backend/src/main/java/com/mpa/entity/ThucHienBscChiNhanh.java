package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "thuc_hien_bsc_chi_nhanh")
@Data
public class ThucHienBscChiNhanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ngay")
    private LocalDate ngay;

    @Column(name = "thang")
    private Integer thang;

    @Column(name = "quy")
    private String quy;

    @Column(name = "nam", nullable = false)
    private Integer nam;

    @Column(name = "ma_cn")
    private String maCn;

    @Column(name = "ten_cn")
    private String tenCn;

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

    @Column(name = "casa_binh_quan")
    private BigDecimal casaBinhQuan;

    @Column(name = "type_data")
    private Integer typeData;

    @Column(name = "ma_don_vi_cap_6")
    private String maDonViCap6;

    @Column(name = "ten_don_vi_cap_6")
    private String tenDonViCap6;

    @Column(name = "ma_am")
    private String maAm;

    @Column(name = "ten_am")
    private String tenAm;
}
