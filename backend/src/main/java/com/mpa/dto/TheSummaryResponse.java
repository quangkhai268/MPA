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
    private BigDecimal doanhSoGiaoDichMienPtn; // SUM(doanh_so_giao_dich_mien_ptn) triệu VND
    private double tyLeDungHanMuc;       // doanhSoGiaoDichMienPtn / hanMucCap * 100 (%)
    private long soTheDatPtn;
    private long soTheChuaDatPtn;
    private long tongSoTdqt;             // Tổng số thẻ loại_the_tin_dung = 'TDQT'
    private long soTdqtDatPtn;           // Số thẻ TDQT đã đạt miễn PTN
}
