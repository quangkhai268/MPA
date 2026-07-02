package com.mpa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KpiSumResult {
    private BigDecimal huyDongVonCuoiKy;
    private BigDecimal casaBinhQuan;
    private BigDecimal duNoTinDungCuoiKy;
    private BigDecimal thuNhapThuanDichVu;
    private BigDecimal thuNhapThuanHdvFtp;
    private BigDecimal thuNhapThuanTinDung;
    private BigDecimal thuNhapThuan;
}
