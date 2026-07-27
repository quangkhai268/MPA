package com.mpa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhachHangDichVuItem {
    private String tenSpCap5;
    private String kyHanCap2;
    private BigDecimal thuNhapThuan;
    private BigDecimal huyDongVonCuoiKy;
    private BigDecimal duNoTinDungCuoiKy;
}
