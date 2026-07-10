package com.mpa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RevenueSeriesPoint {
    private String label;
    private BigDecimal value;
}
