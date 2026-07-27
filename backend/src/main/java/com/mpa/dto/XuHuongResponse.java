package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class XuHuongResponse {
    private List<String> periods;
    private List<XuHuongMetricSeries> metrics;
}
