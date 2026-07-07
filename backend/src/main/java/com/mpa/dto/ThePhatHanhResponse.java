package com.mpa.dto;

import com.mpa.entity.ThePhatHanh;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ThePhatHanhResponse {

    private Long id;
    private String soCifKhachHangPht;
    private String issuingContractNbr;
    private String productCode;
    private String trangThaiIssuingContract;
    private String cifChuTheChinh;
    private String tenChuTheChinh;
    private BigDecimal hmtdIssuingContract;
    private String loaiThe;
    private String soTheDaPhatHanh;
    private String cardId;
    private String trangThaiHienThi;
    private String hinhThucThe;
    private Integer soNgayChuaKichHoat;
    private LocalDate ngayPhatHanhHienThi;
    private String thoiHanHieuLucThe;
    private String amCard;
    private String amIssuingContract;
    // Doanh số giao dịch thực tế (để miễn PTN)
    private BigDecimal doanhSoGiaoDichMienPtn;
    // Mức doanh số cần đạt để miễn phí thường niên (từ cfr.so_tien)
    private BigDecimal doanhSoMienPtn;
    private BigDecimal soTienPhiThuongNien;
    // ptnThreshold = doanhSoMienPtn (hiển thị mức cần đạt)
    private BigDecimal ptnThreshold;
    // % thực hiện = doanhSoGiaoDichMienPtn / doanhSoMienPtn * 100
    private Double pctPtn;
    private LocalDateTime ngayKichHoat;
    private LocalDateTime ngayPsgd;
    private String nhomKhThe;
    private String kenhPhatHanh;
    private String mangLuoi;
    private String loaiTheTinDung;
    private String sdt;
    private String email;

    public static ThePhatHanhResponse from(ThePhatHanh e) {
        ThePhatHanhResponse r = new ThePhatHanhResponse();
        r.id                        = e.getId();
        r.soCifKhachHangPht         = e.getSoCifKhachHangPht();
        r.issuingContractNbr        = e.getIssuingContractNbr();
        r.productCode               = e.getProductCode();
        r.trangThaiIssuingContract  = e.getTrangThaiIssuingContract();
        r.cifChuTheChinh            = e.getCifChuTheChinh();
        r.tenChuTheChinh            = e.getTenChuTheChinh();
        r.hmtdIssuingContract       = e.getHmtdIssuingContract();
        r.loaiThe                   = e.getLoaiThe();
        r.soTheDaPhatHanh           = e.getSoTheDaPhatHanh();
        r.cardId                    = e.getCardId();
        r.hinhThucThe               = e.getHinhThucThe();
        r.soNgayChuaKichHoat        = e.getSoNgayChuaKichHoat();
        r.thoiHanHieuLucThe         = e.getThoiHanHieuLucThe();
        r.amCard                    = e.getAmCard();
        r.amIssuingContract         = e.getAmIssuingContract();
        r.soTienPhiThuongNien       = e.getSoTienPhiThuongNien();
        r.ngayKichHoat              = e.getNgayCapNhatTrangThaiCardContract();
        r.ngayPsgd                  = e.getNgayCapNhatTrangThaiIssuingContract();
        r.nhomKhThe                 = e.getNhomKhThe();
        r.kenhPhatHanh              = e.getKenhPhatHanh();
        r.loaiTheTinDung            = e.getLoaiTheTinDung();
        r.sdt                       = e.getSdt();
        r.email                     = e.getEmail();

        r.trangThaiHienThi = e.getTrangThaiThe();

        if (e.getNgayPhatHanhThe() != null) {
            r.ngayPhatHanhHienThi = e.getNgayPhatHanhThe().toLocalDate();
        } else if (e.getNgayCapNhatTrangThaiIssuingContract() != null) {
            r.ngayPhatHanhHienThi = e.getNgayCapNhatTrangThaiIssuingContract().toLocalDate();
        }

        // Doanh số thực tế
        r.doanhSoGiaoDichMienPtn = e.getDoanhSoGiaoDichMienPtn() != null
                ? e.getDoanhSoGiaoDichMienPtn() : BigDecimal.ZERO;

        // Mức miễn phí thường niên (ngưỡng doanh số)
        r.doanhSoMienPtn = e.getDoanhSoMienPtn() != null
                ? e.getDoanhSoMienPtn() : BigDecimal.ZERO;

        // ptnThreshold dùng doanhSoMienPtn thay vì hmtd*2
        r.ptnThreshold = r.doanhSoMienPtn;

        // % thực hiện = doanhSoGiaoDichMienPtn / doanhSoMienPtn * 100
        r.pctPtn = r.doanhSoMienPtn.compareTo(BigDecimal.ZERO) > 0
                ? r.doanhSoGiaoDichMienPtn
                        .divide(r.doanhSoMienPtn, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        r.mangLuoi = deriveNetwork(e.getHinhThucThe(), e.getProductCode());

        return r;
    }

    private static String deriveNetwork(String hinhThuc, String productCode) {
        String s = ((hinhThuc != null ? hinhThuc : "") + (productCode != null ? productCode : "")).toUpperCase();
        if (s.contains("JCB"))    return "JCB";
        if (s.contains("VISA") || s.contains("VP") || s.contains("VB")) return "Visa";
        if (s.contains("MASTER") || s.contains("MC")) return "Mastercard";
        if (s.contains("AMEX"))   return "Amex";
        if (s.contains("NAPAS"))  return "Napas";
        return "";
    }
}
