package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RevenueSeriesResponse {
    private List<RevenueSeriesPoint> points;
    private BigDecimal tong;
    /** true nếu chưa đủ snapshot để tính delta có ý nghĩa (mới có 0-1 điểm dữ liệu). */
    private boolean chuaDuLieu;
}
