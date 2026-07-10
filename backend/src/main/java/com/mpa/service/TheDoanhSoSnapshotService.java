package com.mpa.service;

import com.mpa.dto.RevenueSeriesResponse;

import java.time.LocalDate;

public interface TheDoanhSoSnapshotService {

    /** Chuỗi doanh số phát sinh (delta giữa các snapshot liên tiếp) của 1 thẻ. */
    RevenueSeriesResponse getSeries(String cardId, String granularity, LocalDate from, LocalDate to);

    /** Tổng hợp doanh số phát sinh toàn danh mục thẻ. */
    RevenueSeriesResponse getBaoCaoTongHop(String granularity, LocalDate from, LocalDate to);
}
