package com.mpa.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TheSummaryResponse {
    private long tongSoThe;
    private long soTheHoatDong;
    private long soTheBiKhoa;
    private long soTheChuaKichHoat;
    private long soTheChuaPsgd;
    private BigDecimal hanMucCap;        // SUM(hmtd_issuing_contract) triệu VND
    private BigDecimal duNo;             // SUM(parsed liab_top_contract) triệu VND
    private double tyLeDungHanMuc;       // duNo / hanMucCap * 100 (%)
    private long soTheDatPtn;
    private long soTheChuaDatPtn;
}
