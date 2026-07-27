package com.mpa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AmTopResponse {
    private String maAm;
    private String tenAm;
    private String tenDonViCap6;
    private BigDecimal thuNhapThuan;
    private BigDecimal duNo;
    private BigDecimal hdvCuoiKy;
    private int soKhachHang;
    /** Giá trị của chỉ tiêu đang được chọn để xếp hạng (VD Dư nợ, CASA...) — dùng để hiển thị. */
    private BigDecimal value;
    /** Giá trị của chỉ tiêu đang chọn ở kỳ trước — null nếu không tính (chưa truyền soVoi). */
    private BigDecimal thuNhapThuanPrevious;
    private BigDecimal change;
    private Double changePercent;
}
