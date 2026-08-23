package com.mpa.repository;

import com.mpa.entity.ThePhatHanh;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ThePhatHanhRepository extends JpaRepository<ThePhatHanh, Long>, JpaSpecificationExecutor<ThePhatHanh> {

    @Query("""
        SELECT t FROM ThePhatHanh t
        WHERE ('' = :search
            OR LOWER(COALESCE(t.soTheDaPhatHanh,'')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(t.tenChuTheChinh,''))  LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(t.soCifKhachHangPht,'')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(t.issuingContractNbr,'')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(t.cifChuTheChinh,'')) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:trangThai IS NULL OR t.trangThaiThe = :trangThai)
        AND (:hinhThuc  IS NULL OR t.hinhThucThe = :hinhThuc)
        AND (:productCode IS NULL OR t.productCode = :productCode)
        AND (
            (:chuaDatPtn = false AND :datPtn = false AND (:loaiTheTinDung IS NULL OR t.loaiTheTinDung = :loaiTheTinDung))
            OR ((:chuaDatPtn = true OR :datPtn = true) AND t.loaiTheTinDung = 'TDQT')
        )
        AND (:maDonViCap6 IS NULL OR t.amIssuingContract IN (
            SELECT a.maAm FROM ThongTinAm a WHERE a.maDonViCap6 = :maDonViCap6))
        AND (:amSearch = ''
            OR LOWER(COALESCE(t.amIssuingContract,'')) LIKE LOWER(CONCAT('%', :amSearch, '%'))
            OR t.amIssuingContract IN (
                SELECT a.maAm FROM ThongTinAm a WHERE LOWER(a.tenAm) LIKE LOWER(CONCAT('%', :amSearch, '%'))))
        AND (:amCodes IS NULL OR t.amIssuingContract IN :amCodes)
        AND (:chuaKichHoat = false OR t.soNgayChuaKichHoat > :soNgayMin)
        AND (:chuaPsgd = false OR (t.soNgayChuaKichHoat = 0 AND (t.doanhSoGiaoDichMienPtn IS NULL OR t.doanhSoGiaoDichMienPtn = 0)))
        AND (
            :chuaDatPtn = :datPtn
            OR (:chuaDatPtn = true
                AND (t.soTienPhiThuongNien IS NULL OR t.soTienPhiThuongNien <> 0)
                AND (t.doanhSoGiaoDichMienPtn IS NULL OR t.doanhSoMienPtn IS NULL OR t.doanhSoGiaoDichMienPtn < t.doanhSoMienPtn))
            OR (:datPtn = true
                AND (t.soTienPhiThuongNien = 0
                    OR (t.doanhSoGiaoDichMienPtn IS NOT NULL AND t.doanhSoMienPtn IS NOT NULL AND t.doanhSoGiaoDichMienPtn >= t.doanhSoMienPtn)))
        )
        AND (
            (:chuaDatPtn = false AND :datPtn = false)
            OR t.trangThaiThe IS NULL
            OR t.trangThaiThe NOT IN ('Card Auto-Closed', 'Card Closed', 'Card Fraud', 'Card Lost')
        )
        AND (:soNgayThuPtn <= 0 OR (t.ngayThuPhiThuongTienTiepTheo IS NOT NULL
            AND t.ngayThuPhiThuongTienTiepTheo BETWEEN FUNCTION('current_date') AND :hanThuPtn))
        ORDER BY t.id DESC
        """)
    Page<ThePhatHanh> search(
            @Param("search") String search,
            @Param("trangThai") String trangThai,
            @Param("hinhThuc") String hinhThuc,
            @Param("productCode") String productCode,
            @Param("loaiTheTinDung") String loaiTheTinDung,
            @Param("maDonViCap6") String maDonViCap6,
            @Param("amSearch") String amSearch,
            @Param("amCodes") List<String> amCodes,
            @Param("chuaKichHoat") boolean chuaKichHoat,
            @Param("soNgayMin") int soNgayMin,
            @Param("chuaPsgd") boolean chuaPsgd,
            @Param("chuaDatPtn") boolean chuaDatPtn,
            @Param("datPtn") boolean datPtn,
            @Param("soNgayThuPtn") int soNgayThuPtn,
            @Param("hanThuPtn") LocalDate hanThuPtn,
            Pageable pageable);

    @Query("SELECT DISTINCT t.trangThaiThe FROM ThePhatHanh t WHERE t.trangThaiThe IS NOT NULL ORDER BY t.trangThaiThe")
    List<String> findDistinctTrangThai();

    @Query("SELECT DISTINCT t.hinhThucThe FROM ThePhatHanh t WHERE t.hinhThucThe IS NOT NULL ORDER BY t.hinhThucThe")
    List<String> findDistinctHinhThuc();

    @Query("SELECT DISTINCT t.productCode FROM ThePhatHanh t WHERE t.productCode IS NOT NULL ORDER BY t.productCode")
    List<String> findDistinctProductCode();

    @Query("SELECT DISTINCT t.loaiThe FROM ThePhatHanh t WHERE t.loaiThe IS NOT NULL ORDER BY t.loaiThe")
    List<String> findDistinctLoaiThe();

    @Query("SELECT DISTINCT t.nhomKhThe FROM ThePhatHanh t WHERE t.nhomKhThe IS NOT NULL ORDER BY t.nhomKhThe")
    List<String> findDistinctNhomKhThe();

    List<ThePhatHanh> findBySoCifKhachHangPhtOrderByIdDesc(String soCifKhachHangPht);

    long countByAmIssuingContractIn(List<String> amCodes);

    @Query("SELECT COUNT(t) FROM ThePhatHanh t WHERE t.soNgayChuaKichHoat > 0")
    long countChuaKichHoat();

    @Query("SELECT COUNT(t) FROM ThePhatHanh t WHERE (t.soNgayChuaKichHoat = 0 OR t.soNgayChuaKichHoat IS NULL) AND (t.doanhSoGiaoDichMienPtn IS NULL OR t.doanhSoGiaoDichMienPtn = 0)")
    long countChuaPsgd();

    // Phí thường niên chỉ tính với thẻ Tín dụng QT và không thuộc 4 trạng thái
    // Auto-Closed/Closed/Fraud/Lost — countChuaDatPtn/countTdqt/countTdqtDatPtn dưới đây
    // đều giới hạn theo đúng phạm vi thẻ "đủ điều kiện xét PTN" này. Thẻ có
    // so_tien_phi_thuong_nien = 0 (miễn phí thường niên luôn) cũng tính là đã đạt, bất kể
    // doanh số giao dịch thực tế.
    @Query("""
        SELECT COUNT(t) FROM ThePhatHanh t
        WHERE t.loaiTheTinDung = 'TDQT'
          AND (t.trangThaiThe IS NULL OR t.trangThaiThe NOT IN ('Card Auto-Closed', 'Card Closed', 'Card Fraud', 'Card Lost'))
          AND (t.soTienPhiThuongNien IS NULL OR t.soTienPhiThuongNien <> 0)
          AND (t.doanhSoGiaoDichMienPtn IS NULL OR t.doanhSoMienPtn IS NULL OR t.doanhSoGiaoDichMienPtn < t.doanhSoMienPtn)
        """)
    long countChuaDatPtn();

    @Query("SELECT COALESCE(SUM(t.hmtdIssuingContract), 0) FROM ThePhatHanh t")
    java.math.BigDecimal sumHanMuc();

    @Query("SELECT COALESCE(SUM(t.doanhSoGiaoDichMienPtn), 0) FROM ThePhatHanh t")
    java.math.BigDecimal sumDoanhSo();

    @Query("""
        SELECT COUNT(t) FROM ThePhatHanh t
        WHERE t.loaiTheTinDung = 'TDQT'
          AND (t.trangThaiThe IS NULL OR t.trangThaiThe NOT IN ('Card Auto-Closed', 'Card Closed', 'Card Fraud', 'Card Lost'))
        """)
    long countTdqt();

    @Query("""
        SELECT COUNT(t) FROM ThePhatHanh t
        WHERE t.loaiTheTinDung = 'TDQT'
          AND (t.trangThaiThe IS NULL OR t.trangThaiThe NOT IN ('Card Auto-Closed', 'Card Closed', 'Card Fraud', 'Card Lost'))
          AND (t.soTienPhiThuongNien = 0
              OR (t.doanhSoGiaoDichMienPtn IS NOT NULL AND t.doanhSoMienPtn IS NOT NULL AND t.doanhSoGiaoDichMienPtn >= t.doanhSoMienPtn))
        """)
    long countTdqtDatPtn();

    // Đếm thẻ "đang khóa" bằng SQL thay vì findAll().stream() — dịch nguyên logic
    // isKhoa() ở ThePhatHanhServiceImpl (chứa 1 trong các chuỗi KHÓA/KHOA/BLOCK/BLK).
    @Query("""
        SELECT COUNT(t) FROM ThePhatHanh t
        WHERE UPPER(t.trangThaiIssuingContract) LIKE '%KHÓA%'
           OR UPPER(t.trangThaiIssuingContract) LIKE '%KHOA%'
           OR UPPER(t.trangThaiIssuingContract) LIKE '%BLOCK%'
           OR UPPER(t.trangThaiIssuingContract) LIKE '%BLK%'
        """)
    long countBiKhoa();

    // ── Dùng cho job cảnh báo gửi email (không đụng tới search() ở trên) ──

    @Query("""
        SELECT t FROM ThePhatHanh t
        WHERE t.soNgayChuaKichHoat > :soNgayMin
          AND t.email IS NOT NULL AND t.email <> ''
        """)
    List<ThePhatHanh> findChuaKichHoatForNotify(@Param("soNgayMin") int soNgayMin);

    @Query("""
        SELECT t FROM ThePhatHanh t
        WHERE t.soNgayChuaKichHoat = 0
          AND (t.doanhSoGiaoDichMienPtn IS NULL OR t.doanhSoGiaoDichMienPtn = 0)
          AND t.ngayCapNhatTrangThaiCardContract IS NOT NULL
          AND t.ngayCapNhatTrangThaiCardContract <= :nguong
          AND t.email IS NOT NULL AND t.email <> ''
        """)
    List<ThePhatHanh> findChuaPsgdForNotify(@Param("nguong") LocalDateTime nguong);

    @Query("""
        SELECT t FROM ThePhatHanh t
        WHERE t.ngayPhatHanhThe IS NOT NULL
          AND FUNCTION('DATE', t.ngayPhatHanhThe) = :targetDate
          AND t.email IS NOT NULL AND t.email <> ''
        """)
    List<ThePhatHanh> findByNgayPhatHanhDate(@Param("targetDate") LocalDate targetDate);
}
