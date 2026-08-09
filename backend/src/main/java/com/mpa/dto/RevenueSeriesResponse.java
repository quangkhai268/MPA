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
    /** true nếu chưa có snapshot nào trong khoảng lọc. */
    private boolean chuaDuLieu;
}
