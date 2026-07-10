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
        AND (:loaiTheTinDung IS NULL OR t.loaiTheTinDung = :loaiTheTinDung)
        AND (:chuaKichHoat = false OR t.soNgayChuaKichHoat > :soNgayMin)
        AND (:chuaPsgd = false OR (t.soNgayChuaKichHoat = 0 AND (t.doanhSoGiaoDichMienPtn IS NULL OR t.doanhSoGiaoDichMienPtn = 0)))
        AND (:chuaDatPtn = false OR (t.doanhSoGiaoDichMienPtn IS NULL OR t.doanhSoMienPtn IS NULL OR t.doanhSoGiaoDichMienPtn < t.doanhSoMienPtn))
        ORDER BY t.id DESC
        """)
    Page<ThePhatHanh> search(
            @Param("search") String search,
            @Param("trangThai") String trangThai,
            @Param("hinhThuc") String hinhThuc,
            @Param("productCode") String productCode,
            @Param("loaiTheTinDung") String loaiTheTinDung,
            @Param("chuaKichHoat") boolean chuaKichHoat,
            @Param("soNgayMin") int soNgayMin,
            @Param("chuaPsgd") boolean chuaPsgd,
            @Param("chuaDatPtn") boolean chuaDatPtn,
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

    @Query("SELECT COUNT(t) FROM ThePhatHanh t WHERE t.soNgayChuaKichHoat > 0")
    long countChuaKichHoat();

    @Query("SELECT COUNT(t) FROM ThePhatHanh t WHERE (t.soNgayChuaKichHoat = 0 OR t.soNgayChuaKichHoat IS NULL) AND (t.doanhSoGiaoDichMienPtn IS NULL OR t.doanhSoGiaoDichMienPtn = 0)")
    long countChuaPsgd();

    @Query("SELECT COUNT(t) FROM ThePhatHanh t WHERE t.doanhSoGiaoDichMienPtn IS NULL OR t.doanhSoMienPtn IS NULL OR t.doanhSoGiaoDichMienPtn < t.doanhSoMienPtn")
    long countChuaDatPtn();

    @Query("SELECT COALESCE(SUM(t.hmtdIssuingContract), 0) FROM ThePhatHanh t")
    java.math.BigDecimal sumHanMuc();

    @Query("SELECT COALESCE(SUM(t.doanhSoGiaoDichMienPtn), 0) FROM ThePhatHanh t")
    java.math.BigDecimal sumDoanhSo();

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
