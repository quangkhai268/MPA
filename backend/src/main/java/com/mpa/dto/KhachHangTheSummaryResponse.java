package com.mpa.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class KhachHangTheSummaryResponse {
    private int soHopDong;
    private int soKhoa;
    private int soChuaActive;
    private BigDecimal tongHanMuc;          // SUM(hmtd_issuing_contract)
    private BigDecimal tongDoanhSo;         // SUM(doanh_so_giao_dich_mien_ptn) — KHÔNG PHẢI dư nợ thật
    private double tyLeDoanhSoTrenHanMuc;   // tongDoanhSo / tongHanMuc * 100 (%)
    private int soTheChuaDatPtn;
    private List<ThePhatHanhResponse> theList;
}
