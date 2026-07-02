package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PhongTrendSeriesResponse {
    private String tenDonViCap6;
    private List<BigDecimal> values;
}
