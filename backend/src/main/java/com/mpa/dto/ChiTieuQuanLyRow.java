package com.mpa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChiTieuQuanLyRow {
    private Integer id;
    private String maUnit;
    private String tenUnit;
    private BigDecimal hdvCuoiKy;
    private BigDecimal casaBinhQuan;
    private BigDecimal duNoTinDung;
    private BigDecimal tntDichVu;
    private BigDecimal tntHdvFtp;
    private BigDecimal tntTinDung;
    private BigDecimal thuNhapThuan;
    private LocalDateTime ngayTao;
}
