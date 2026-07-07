package com.mpa.dto;

import com.mpa.entity.ThePhatHanh;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ThePhatHanhDetailResponse {

    private Long id;
    private String soTheDaPhatHanh;
    private String productCode;
    private LocalDate ngayPhatHanhHienThi;
    private String trangThaiHienThi;
    private String tenChuTheChinh;
    private String thoiHanHieuLucThe;
    private String mangLuoi;
    private String loaiThe;
    private String hinhThucThe;
    private String plasticStatus;
    private String lyDoPhatHanh;
    private String kenhPhatHanh;
    private String nhomKhThe;
    private String dacQuyenThe;
    private Integer soNgayChuaKichHoat;

    private String issuingContractNbr;
    private BigDecimal hmtdIssuingContract;
    private LocalDate thoiHanHmtd;
    private String trangThaiIssuingContract;
    private String amIssuingContract;
    private String cnQlt;
    private String liabTopContract;

    private String soCifKhachHangPht;
    private String hoTenKhachHangPht;
    private String cifChuTheChinh;
    private String sdt;
    private String email;
    private String soGttt;
    private String sinhTracHocKhachHang;

    private BigDecimal doanhSoGiaoDichMienPtn;
    private BigDecimal doanhSoMienPtn;
    private Double pctPtn;
    private BigDecimal soTienPhiThuongNien;
    private String mucPhiThuongNienThe;

    private LocalDateTime ngayKichHoat;
    private LocalDateTime ngayPsgd;
    private LocalDateTime ngayPhatHanhThe;

    public static ThePhatHanhDetailResponse from(ThePhatHanh e) {
        ThePhatHanhDetailResponse r = new ThePhatHanhDetailResponse();
        r.id                       = e.getId();
        r.soTheDaPhatHanh          = e.getSoTheDaPhatHanh();
        r.productCode              = e.getProductCode();
        r.trangThaiHienThi         = e.getTrangThaiThe();
        r.tenChuTheChinh           = e.getTenChuTheChinh();
        r.thoiHanHieuLucThe        = e.getThoiHanHieuLucThe();
        r.loaiThe                  = e.getLoaiThe();
        r.hinhThucThe              = e.getHinhThucThe();
        r.plasticStatus            = e.getPlasticStatus();
        r.lyDoPhatHanh             = e.getLyDoPhatHanh();
        r.kenhPhatHanh             = e.getKenhPhatHanh();
        r.nhomKhThe                = e.getNhomKhThe();
        r.dacQuyenThe              = e.getDacQuyenThe();
        r.soNgayChuaKichHoat       = e.getSoNgayChuaKichHoat();

        r.issuingContractNbr       = e.getIssuingContractNbr();
        r.hmtdIssuingContract      = e.getHmtdIssuingContract();
        r.thoiHanHmtd              = e.getThoiHanHmtd();
        r.trangThaiIssuingContract = e.getTrangThaiIssuingContract();
        r.amIssuingContract        = e.getAmIssuingContract();
        r.cnQlt                    = e.getCnQlt();
        r.liabTopContract          = e.getLiabTopContract();

        r.soCifKhachHangPht        = e.getSoCifKhachHangPht();
        r.hoTenKhachHangPht        = e.getHoTenKhachHangPht();
        r.cifChuTheChinh           = e.getCifChuTheChinh();
        r.sdt                      = e.getSdt();
        r.email                    = e.getEmail();
        r.soGttt                   = e.getSoGttt();
        r.sinhTracHocKhachHang     = e.getSinhTracHocKhachHang();

        r.soTienPhiThuongNien      = e.getSoTienPhiThuongNien();
        r.mucPhiThuongNienThe      = e.getMucPhiThuongNienThe();

        r.ngayKichHoat             = e.getNgayCapNhatTrangThaiCardContract();
        r.ngayPsgd                 = e.getNgayCapNhatTrangThaiIssuingContract();
        r.ngayPhatHanhThe          = e.getNgayPhatHanhThe();

        if (e.getNgayPhatHanhThe() != null) {
            r.ngayPhatHanhHienThi = e.getNgayPhatHanhThe().toLocalDate();
        } else if (e.getNgayCapNhatTrangThaiIssuingContract() != null) {
            r.ngayPhatHanhHienThi = e.getNgayCapNhatTrangThaiIssuingContract().toLocalDate();
        }

        r.doanhSoGiaoDichMienPtn = e.getDoanhSoGiaoDichMienPtn() != null
                ? e.getDoanhSoGiaoDichMienPtn() : BigDecimal.ZERO;
        r.doanhSoMienPtn = e.getDoanhSoMienPtn() != null
                ? e.getDoanhSoMienPtn() : BigDecimal.ZERO;
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
