package com.mpa.service;

import com.mpa.dto.AmDetailResponse;
import com.mpa.dto.DashboardKpiResponse;
import com.mpa.dto.PhongDataResponse;
import com.mpa.dto.PhongTrendSeriesResponse;
import com.mpa.dto.TrendDataResponse;
import java.util.List;

public interface DashboardService {
    DashboardKpiResponse getKpi(String loaiKy, String selectedKy, String soVoi);
    TrendDataResponse getTrend(String loaiKy, String selectedKy, String metricKey);
    List<PhongDataResponse> getPhongData(String loaiKy, String selectedKy, String soVoi);
    List<PhongTrendSeriesResponse> getPhongTrend(String loaiKy, String selectedKy, String metricKey);
    List<AmDetailResponse> getAmDetail(String loaiKy, String selectedKy, String maDonViCap6, String soVoi);
}
