package com.mpa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AmDetailResponse {
    private String maAm;
    private String tenAm;
    private String tenDonViCap6;
    // Giá trị kỳ hiện tại
    private BigDecimal hdvCuoiKy;
    private BigDecimal hdvBinhQuan;
    private BigDecimal duNo;
    private BigDecimal thuNhapThuanDichVu;
    private BigDecimal thuNhapThuanHdvFtp;
    private BigDecimal thuNhapThuanTinDung;
    private BigDecimal thuNhapThuan;
    // % thay đổi so kỳ trước (null = không có dữ liệu kỳ trước)
    private Double hdvCuoiKyChangePct;
    private Double hdvBinhQuanChangePct;
    private Double duNoChangePct;
    private Double tntDichVuChangePct;
    private Double tntHdvChangePct;
    private Double tntTinDungChangePct;
    private Double thuNhapThuanChangePct;
}
