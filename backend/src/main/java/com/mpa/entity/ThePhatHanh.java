package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "v_the_phat_hanh")
@Data
public class ThePhatHanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cn_qlt")
    private String cnQlt;

    @Column(name = "so_cif_khach_hang_pht")
    private String soCifKhachHangPht;

    @Column(name = "ho_ten_khach_hang_pht")
    private String hoTenKhachHangPht;

    @Column(name = "sinh_trac_hoc_khach_hang")
    private String sinhTracHocKhachHang;

    @Column(name = "sdt")
    private String sdt;

    @Column(name = "email")
    private String email;

    @Column(name = "thoi_han_hmtd")
    private LocalDate thoiHanHmtd;

    @Column(name = "issuing_contract_nbr")
    private String issuingContractNbr;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "trang_thai_issuing_contract")
    private String trangThaiIssuingContract;

    @Column(name = "cif_chu_the_chinh")
    private String cifChuTheChinh;

    @Column(name = "ten_chu_the_chinh")
    private String tenChuTheChinh;

    @Column(name = "hmtd_issuing_contract")
    private BigDecimal hmtdIssuingContract;

    @Column(name = "am_issuing_contract")
    private String amIssuingContract;

    @Column(name = "loai_the")
    private String loaiThe;

    @Column(name = "so_the_da_phat_hanh")
    private String soTheDaPhatHanh;

    @Column(name = "card_id")
    private String cardId;

    @Column(name = "trang_thai_the")
    private String trangThaiThe;

    @Column(name = "hinh_thuc_the")
    private String hinhThucThe;

    @Column(name = "plastic_status")
    private String plasticStatus;

    @Column(name = "so_ngay_chua_kich_hoat")
    private Integer soNgayChuaKichHoat;

    @Column(name = "ngay_phat_hanh_the")
    private LocalDateTime ngayPhatHanhThe;

    @Column(name = "thoi_han_hieu_luc_the")
    private String thoiHanHieuLucThe;

    @Column(name = "am_card")
    private String amCard;

    @Column(name = "ma_can_bo_gioi_thieu")
    private String maCanBoGioiThieu;

    @Column(name = "doanh_so_giao_dich_mien_ptn")
    private BigDecimal doanhSoGiaoDichMienPtn;

    @Column(name = "ngay_thu_phi_thuong_nien_gan_nhat")
    private LocalDate ngayThuPhiThuongNienGanNhat;

    @Column(name = "muc_phi_thuong_nien_the")
    private String mucPhiThuongNienThe;

    @Column(name = "so_tien_phi_thuong_nien")
    private BigDecimal soTienPhiThuongNien;

    @Column(name = "dac_quyen_the")
    private String dacQuyenThe;

    @Column(name = "kenh_phat_hanh")
    private String kenhPhatHanh;

    @Column(name = "nhom_kh_the")
    private String nhomKhThe;

    @Column(name = "ngay_cap_nhat_trang_thai_issuing_contract")
    private LocalDateTime ngayCapNhatTrangThaiIssuingContract;

    @Column(name = "ngay_cap_nhat_trang_thai_card_contract")
    private LocalDateTime ngayCapNhatTrangThaiCardContract;

    @Column(name = "client_code")
    private String clientCode;

    @Column(name = "thuoc_client_code")
    private String thuocClientCode;

    @Column(name = "khac_client_code")
    private String khacClientCode;

    @Column(name = "loai_match")
    private String loaiMatch;

    // cfr.so_tien AS doanh_so_mien_ptn – mức doanh số để được miễn phí thường niên
    @Column(name = "doanh_so_mien_ptn")
    private BigDecimal doanhSoMienPtn;

    // Computed in view: 'TDQT' hoặc 'KHAC'
    @Column(name = "loai_the_tin_dung")
    private String loaiTheTinDung;

    @Column(name = "ly_do_phat_hanh")
    private String lyDoPhatHanh;

    @Column(name = "liab_top_contract")
    private String liabTopContract;

    @Column(name = "so_gttt")
    private String soGttt;
}
