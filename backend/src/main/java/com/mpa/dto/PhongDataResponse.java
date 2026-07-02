package com.mpa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PhongDataResponse {
    private String maDonViCap6;
    private String tenDonViCap6;
    // Current values
    private BigDecimal thuNhapThuan;
    private BigDecimal thuNhapThuanPrevious;
    private BigDecimal duNo;
    private BigDecimal hdvCuoiKy;
    private BigDecimal hdvBinhQuan;
    private BigDecimal thuNhapThuanDichVu;
    private BigDecimal thuNhapThuanHdvFtp;
    private BigDecimal thuNhapThuanTinDung;
    private int soKhachHang;
    private int soAm;
    // % change vs previous period (null = no previous data)
    private Double changePercent;
    private Double hdvCuoiKyChangePercent;
    private Double hdvBinhQuanChangePercent;
    private Double duNoChangePercent;
    private Double tntDichVuChangePercent;
    private Double tntHdvChangePercent;
    private Double tntTinDungChangePercent;
}
