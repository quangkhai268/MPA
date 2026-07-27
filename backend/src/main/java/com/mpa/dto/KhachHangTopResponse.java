package com.mpa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class KhachHangTopResponse {
    private String maKhCif;
    private String tenKhachHang;
    private String tenDonViCap6;
    private String tenAm;
    private BigDecimal thuNhapThuan;
    private BigDecimal thuNhapThuanPrevious;
    private BigDecimal change;
    private Double changePercent;
    /** Giá trị của chỉ tiêu đang được chọn để xếp hạng (VD Dư nợ, CASA...) — dùng để hiển thị. */
    private BigDecimal value;
}
