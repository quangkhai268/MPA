package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardKpiResponse {
    // Kỳ hiện tại (triệu VND)
    private BigDecimal tongHdvCuoiKy;
    private BigDecimal tongHdvBinhQuan;   // CASA bình quân
    private BigDecimal tongDuNo;
    private BigDecimal tongTntDichVu;
    private BigDecimal tongTntHdvFtp;
    private BigDecimal tongTntTinDung;
    private BigDecimal tongThuNhapThuan;

    // Kỳ trước (triệu VND)
    private BigDecimal tongHdvCuoiKyPrev;
    private BigDecimal tongHdvBinhQuanPrev;
    private BigDecimal tongDuNoPrev;
    private BigDecimal tongTntDichVuPrev;
    private BigDecimal tongTntHdvFtpPrev;
    private BigDecimal tongTntTinDungPrev;
    private BigDecimal tongThuNhapThuanPrev;

    // Kế hoạch BSC năm (triệu VND)
    private BigDecimal khHdvCuoiKy;
    private BigDecimal khHdvBinhQuan;
    private BigDecimal khDuNo;
    private BigDecimal khTntDichVu;
    private BigDecimal khTntHdvFtp;
    private BigDecimal khTntTinDung;
    private BigDecimal khTnt;

    // Thống kê tổng hợp
    private int soKhachHang;
    private int soAm;
    private int soPhong;
}
