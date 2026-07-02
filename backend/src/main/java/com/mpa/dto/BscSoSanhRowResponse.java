package com.mpa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BscSoSanhRowResponse {
    private String maUnit;
    private String tenUnit;

    // HĐV Cuối kỳ
    private BigDecimal hdvCuoiKyTh;
    private BigDecimal hdvCuoiKyKh;
    private Double hdvCuoiKyPct;
    private Double hdvCuoiKyDelta;

    // CASA Bình quân
    private BigDecimal casaBinhQuanTh;
    private BigDecimal casaBinhQuanKh;
    private Double casaBinhQuanPct;
    private Double casaBinhQuanDelta;

    // Dư nợ tín dụng
    private BigDecimal duNoTh;
    private BigDecimal duNoKh;
    private Double duNoPct;
    private Double duNoDelta;

    // TNT Dịch vụ
    private BigDecimal tntDichVuTh;
    private BigDecimal tntDichVuKh;
    private Double tntDichVuPct;
    private Double tntDichVuDelta;

    // TNT HĐV FTP
    private BigDecimal tntHdvTh;
    private BigDecimal tntHdvKh;
    private Double tntHdvPct;
    private Double tntHdvDelta;

    // TNT Tín dụng
    private BigDecimal tntTinDungTh;
    private BigDecimal tntTinDungKh;
    private Double tntTinDungPct;
    private Double tntTinDungDelta;

    // Tổng TNT
    private BigDecimal tntTh;
    private BigDecimal tntKh;
    private Double tntPct;
    private Double tntDelta;
}
