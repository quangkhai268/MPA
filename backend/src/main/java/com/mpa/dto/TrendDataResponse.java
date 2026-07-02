package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TrendDataResponse {
    private int currentYear;
    private int prevYear;        // 0 = không có line kỳ trước (ví dụ loaiKy=nam)
    private List<String> labels;
    private List<BigDecimal> currentValues;
    private List<BigDecimal> prevValues;
}
