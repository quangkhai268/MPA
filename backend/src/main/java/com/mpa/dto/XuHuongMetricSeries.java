package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class XuHuongMetricSeries {
    private String metricKey;
    private String metricLabel;
    private List<BigDecimal> keHoachValues;
    private List<BigDecimal> thucHienValues;
}
