package com.mpa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Chi tiết 1 mã AM (hoặc gộp nhiều mã AM của cùng 1 cán bộ) — dùng cho màn hình
 * chi tiết ở menu quan-ly-am. 7 chỉ tiêu Th/Kh/Pct/Delta khớp field-naming với
 * {@link BscSoSanhRowResponse} để tái dùng chung cách hiển thị (badge màu, format...).
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AmChiTietResponse {
    private List<String> maAmList;
    private boolean isCanBo;

    // Thông tin cơ bản (từ thong_tin_am)
    private String tenAm;
    private String maDonViCap6;
    private String tenDonViCap6;
    private String tenCn;
    private String chucVu;
    private String email;
    private String soDienThoai;
    private LocalDate ngayBatDau;
    private Short trangThai;

    // Số thẻ đang quản lý (the_phat_hanh.am_issuing_contract IN maAmList)
    private long soThe;

    // HĐV Cuối kỳ
    private BigDecimal hdvCuoiKyTh;
    private BigDecimal hdvCuoiKyKh;
    private Double hdvCuoiKyPct;
    private Double hdvCuoiKyDelta;

    // CASA Bình quân
    private BigDecimal casaBinhQuanTh;
    private BigDecimal casaBinhQuanKh;
    private Double casaBinhQuanPct;
    private Double casaBinhQuanDelta;

    // Dư nợ tín dụng
    private BigDecimal duNoTh;
    private BigDecimal duNoKh;
    private Double duNoPct;
    private Double duNoDelta;

    // TNT Dịch vụ
    private BigDecimal tntDichVuTh;
    private BigDecimal tntDichVuKh;
    private Double tntDichVuPct;
    private Double tntDichVuDelta;

    // TNT HĐV FTP
    private BigDecimal tntHdvTh;
    private BigDecimal tntHdvKh;
    private Double tntHdvPct;
    private Double tntHdvDelta;

    // TNT Tín dụng
    private BigDecimal tntTinDungTh;
    private BigDecimal tntTinDungKh;
    private Double tntTinDungPct;
    private Double tntTinDungDelta;

    // Tổng TNT
    private BigDecimal tntTh;
    private BigDecimal tntKh;
    private Double tntPct;
    private Double tntDelta;
}
