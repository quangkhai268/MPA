package com.mpa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhachHangChiTietResponse {

    // ── Thông tin khách hàng ─────────────────────────────────────────────
    private String maKhCif;
    private String tenKhachHang;
    private Integer typeKhachHang;
    private String tenPhanKhuc;
    private String maAm;
    private String tenAm;
    private String maDonViCap6;
    private String tenDonViCap6;
    private String soDienThoai;
    private String email;
    private String ngayBatDau;

    // ── Kỳ hiện tại (triệu VND) ──────────────────────────────────────────
    private BigDecimal hdvCuoiKy;
    private BigDecimal casaBinhQuan;
    private BigDecimal duNo;
    private BigDecimal tntDichVu;
    private BigDecimal tntHdv;
    private BigDecimal tntTinDung;
    private BigDecimal tongTnt;

    // ── Kỳ trước (triệu VND) ─────────────────────────────────────────────
    private BigDecimal hdvCuoiKyPrev;
    private BigDecimal casaBinhQuanPrev;
    private BigDecimal duNoPrev;
    private BigDecimal tntDichVuPrev;
    private BigDecimal tntHdvPrev;
    private BigDecimal tntTinDungPrev;
    private BigDecimal tongTntPrev;

    // ── Trung bình phân khúc (triệu VND) ─────────────────────────────────
    private BigDecimal hdvCuoiKyTb;
    private BigDecimal casaBinhQuanTb;
    private BigDecimal duNoTb;
    private BigDecimal tntDichVuTb;
    private BigDecimal tntHdvTb;
    private BigDecimal tntTinDungTb;
    private BigDecimal tongTntTb;

    // ── Xu hướng theo tháng (sparkline) ──────────────────────────────────
    private List<String> trendLabels;
    private List<BigDecimal> hdvCuoiKyTrend;
    private List<BigDecimal> casaBinhQuanTrend;
    private List<BigDecimal> duNoTrend;
    private List<BigDecimal> tntDichVuTrend;
    private List<BigDecimal> tntHdvTrend;
    private List<BigDecimal> tntTinDungTrend;
    private List<BigDecimal> tongTntTrend;

    // ── Mô tả kỳ ─────────────────────────────────────────────────────────
    private String kyHienTai;
    private String kyTruoc;
}
