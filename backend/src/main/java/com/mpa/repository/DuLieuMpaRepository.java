package com.mpa.repository;

import com.mpa.dto.DuLieuMpaSummary;
import com.mpa.dto.KpiSumResult;
import com.mpa.entity.DuLieuMpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface DuLieuMpaRepository extends JpaRepository<DuLieuMpa, Integer> {

    // ── KPI sum queries – thực tế từ du_lieu_mpa ──────────────────

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(d.huyDongVonCuoiKy),      0), " +
           "COALESCE(SUM(d.huyDongVonBinhQuan),     0), " +  // CASA proxy
           "COALESCE(SUM(d.duNoTinDungCuoiKy),      0), " +
           "COALESCE(SUM(d.thuNhapThuanDichVu),     0), " +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),     0), " +
           "COALESCE(SUM(d.thuNhapThuanTinDung),    0), " +
           "COALESCE(SUM(d.thuNhapThuan),           0)) " +
           "FROM DuLieuMpa d WHERE d.thang = :thang AND d.nam = :nam AND d.loaiKy = 'THANG'")
    KpiSumResult sumByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(d.huyDongVonCuoiKy),      0), " +
           "COALESCE(SUM(d.huyDongVonBinhQuan),     0), " +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),      0), " +
           "COALESCE(SUM(d.thuNhapThuanDichVu),     0), " +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),     0), " +
           "COALESCE(SUM(d.thuNhapThuanTinDung),    0), " +
           "COALESCE(SUM(d.thuNhapThuan),           0)) " +
           "FROM DuLieuMpa d WHERE d.quy = :quy AND d.nam = :nam AND d.loaiKy = 'QUY'")
    KpiSumResult sumByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(d.huyDongVonCuoiKy),      0), " +
           "COALESCE(SUM(d.huyDongVonBinhQuan),     0), " +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),      0), " +
           "COALESCE(SUM(d.thuNhapThuanDichVu),     0), " +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),     0), " +
           "COALESCE(SUM(d.thuNhapThuanTinDung),    0), " +
           "COALESCE(SUM(d.thuNhapThuan),           0)) " +
           "FROM DuLieuMpa d WHERE d.thang = :thang AND d.nam = :nam AND d.loaiKy = 'NAM'")
    KpiSumResult sumByNamLuyKe(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(d.huyDongVonCuoiKy),      0), " +
           "COALESCE(SUM(d.huyDongVonBinhQuan),     0), " +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),      0), " +
           "COALESCE(SUM(d.thuNhapThuanDichVu),     0), " +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),     0), " +
           "COALESCE(SUM(d.thuNhapThuanTinDung),    0), " +
           "COALESCE(SUM(d.thuNhapThuan),           0)) " +
           "FROM DuLieuMpa d WHERE d.ngay = :ngay AND d.loaiKy = 'NGAY'")
    KpiSumResult sumByNgay(@Param("ngay") LocalDate ngay);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(d.huyDongVonCuoiKy),      0), " +
           "COALESCE(SUM(d.huyDongVonBinhQuan),     0), " +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),      0), " +
           "COALESCE(SUM(d.thuNhapThuanDichVu),     0), " +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),     0), " +
           "COALESCE(SUM(d.thuNhapThuanTinDung),    0), " +
           "COALESCE(SUM(d.thuNhapThuan),           0)) " +
           "FROM DuLieuMpa d WHERE d.maKhCif = :cif AND d.ngay = :ngay AND d.loaiKy = 'NGAY'")
    KpiSumResult sumByCifAndNgay(@Param("cif") String cif, @Param("ngay") LocalDate ngay);

    @Query("SELECT MAX(d.ngay) FROM DuLieuMpa d WHERE d.loaiKy = 'NGAY'")
    LocalDate findLatestNgay();

    // ── Dịch vụ/sản phẩm khách hàng đang dùng theo kỳ (menu KH 360°) ────────
    @Query("SELECT d.tenSpCap5, d.kyHanCap2, " +
           "COALESCE(SUM(d.thuNhapThuan),0), COALESCE(SUM(d.huyDongVonCuoiKy),0), COALESCE(SUM(d.duNoTinDungCuoiKy),0) " +
           "FROM DuLieuMpa d WHERE d.maKhCif = :cif AND d.loaiKy = :loaiKy " +
           "AND (:thang IS NULL OR d.thang = :thang) " +
           "AND (:quy IS NULL OR d.quy = :quy) " +
           "AND (:nam IS NULL OR d.nam = :nam) " +
           "AND (:ngay IS NULL OR d.ngay = :ngay) " +
           "GROUP BY d.tenSpCap5, d.kyHanCap2 " +
           "ORDER BY SUM(d.thuNhapThuan) DESC")
    List<Object[]> findDichVuByCif(@Param("cif") String cif, @Param("loaiKy") String loaiKy,
                                    @Param("thang") Integer thang, @Param("quy") String quy,
                                    @Param("nam") Integer nam, @Param("ngay") LocalDate ngay);

    // ── Trend queries – trả về Object[] [period, hdv, casa, duno, tntdv, tnthdv, tnttd, tnt]

    @Query("SELECT d.thang, " +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),   COALESCE(SUM(d.huyDongVonBinhQuan),0), " +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),   COALESCE(SUM(d.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),  COALESCE(SUM(d.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(d.thuNhapThuan),0) " +
           "FROM DuLieuMpa d WHERE d.nam = :nam AND d.loaiKy = 'THANG' " +
           "GROUP BY d.thang ORDER BY d.thang")
    List<Object[]> trendByThang(@Param("nam") int nam);

    @Query("SELECT d.quy, " +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),   COALESCE(SUM(d.huyDongVonBinhQuan),0), " +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),   COALESCE(SUM(d.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),  COALESCE(SUM(d.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(d.thuNhapThuan),0) " +
           "FROM DuLieuMpa d WHERE d.nam = :nam AND d.loaiKy = 'QUY' " +
           "GROUP BY d.quy ORDER BY d.quy")
    List<Object[]> trendByQuy(@Param("nam") int nam);

    @Query("SELECT d.nam, " +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),   COALESCE(SUM(d.huyDongVonBinhQuan),0), " +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),   COALESCE(SUM(d.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),  COALESCE(SUM(d.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(d.thuNhapThuan),0) " +
           "FROM DuLieuMpa d WHERE d.nam IN :years AND d.loaiKy = 'NAM' " +
           "GROUP BY d.nam ORDER BY d.nam")
    List<Object[]> trendByNam(@Param("years") List<Integer> years);

    @Query("SELECT d.ngay, " +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),   COALESCE(SUM(d.huyDongVonBinhQuan),0), " +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),   COALESCE(SUM(d.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),  COALESCE(SUM(d.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(d.thuNhapThuan),0) " +
           "FROM DuLieuMpa d WHERE d.thang = :thang AND d.nam = :nam " +
           "GROUP BY d.ngay ORDER BY d.ngay")
    List<Object[]> trendByNgay(@Param("thang") int thang, @Param("nam") int nam);

    // ── Phong KH count – Object[]: [maDonViCap6, soKhachHang (COUNT DISTINCT maKhCif)] ─
    @Query("SELECT d.maDonViCap6, COUNT(DISTINCT d.maKhCif) " +
           "FROM DuLieuMpa d WHERE d.thang = :thang AND d.nam = :nam AND d.loaiKy = 'THANG' " +
           "GROUP BY d.maDonViCap6")
    List<Object[]> phongKhCountByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT d.maDonViCap6, COUNT(DISTINCT d.maKhCif) " +
           "FROM DuLieuMpa d WHERE d.quy = :quy AND d.nam = :nam AND d.loaiKy = 'QUY' " +
           "GROUP BY d.maDonViCap6")
    List<Object[]> phongKhCountByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT d.maDonViCap6, COUNT(DISTINCT d.maKhCif) " +
           "FROM DuLieuMpa d WHERE d.thang = :thang AND d.nam = :nam AND d.loaiKy = 'NAM' " +
           "GROUP BY d.maDonViCap6")
    List<Object[]> phongKhCountByNamLuyKe(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT MAX(d.thang) FROM DuLieuMpa d WHERE d.loaiKy = 'NAM' AND d.nam = :nam")
    Integer findLatestThangForNamLuyKe(@Param("nam") int nam);

    @Query("SELECT DISTINCT d.thang FROM DuLieuMpa d WHERE d.loaiKy = 'NAM' AND d.nam = :nam ORDER BY d.thang DESC")
    List<Integer> findAvailableThangForNamLuyKe(@Param("nam") int nam);

    // ── Per-CIF KPI sum ────────────────────────────────────────────────

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),COALESCE(SUM(d.huyDongVonBinhQuan),0)," +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),COALESCE(SUM(d.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),COALESCE(SUM(d.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(d.thuNhapThuan),0)) " +
           "FROM DuLieuMpa d WHERE d.maKhCif=:cif AND d.thang=:thang AND d.nam=:nam AND d.loaiKy = 'THANG'")
    KpiSumResult cifSumByThangNam(@Param("cif") String cif, @Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),COALESCE(SUM(d.huyDongVonBinhQuan),0)," +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),COALESCE(SUM(d.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),COALESCE(SUM(d.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(d.thuNhapThuan),0)) " +
           "FROM DuLieuMpa d WHERE d.maKhCif=:cif AND d.quy=:quy AND d.nam=:nam AND d.loaiKy = 'QUY'")
    KpiSumResult cifSumByQuyNam(@Param("cif") String cif, @Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),COALESCE(SUM(d.huyDongVonBinhQuan),0)," +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),COALESCE(SUM(d.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),COALESCE(SUM(d.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(d.thuNhapThuan),0)) " +
           "FROM DuLieuMpa d WHERE d.maKhCif=:cif AND d.thang=:thang AND d.nam=:nam AND d.loaiKy = 'NAM'")
    KpiSumResult cifSumByNamLuyKe(@Param("cif") String cif, @Param("thang") int thang, @Param("nam") int nam);

    // ── Benchmark: avg per CIF within same maDonViCap6 ────────────────
    // Object[]: [0]=count(distinct cif), [1..7]=SUM of 7 metrics

    @Query("SELECT COUNT(DISTINCT d.maKhCif)," +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),COALESCE(SUM(d.huyDongVonBinhQuan),0)," +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),COALESCE(SUM(d.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),COALESCE(SUM(d.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(d.thuNhapThuan),0) " +
           "FROM DuLieuMpa d WHERE d.maDonViCap6=:maPhong AND d.thang=:thang AND d.nam=:nam AND d.loaiKy = 'THANG'")
    List<Object[]> phongBenchmarkByThangNam(@Param("maPhong") String maPhong, @Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT COUNT(DISTINCT d.maKhCif)," +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),COALESCE(SUM(d.huyDongVonBinhQuan),0)," +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),COALESCE(SUM(d.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),COALESCE(SUM(d.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(d.thuNhapThuan),0) " +
           "FROM DuLieuMpa d WHERE d.maDonViCap6=:maPhong AND d.quy=:quy AND d.nam=:nam AND d.loaiKy = 'QUY'")
    List<Object[]> phongBenchmarkByQuyNam(@Param("maPhong") String maPhong, @Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT COUNT(DISTINCT d.maKhCif)," +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),COALESCE(SUM(d.huyDongVonBinhQuan),0)," +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),COALESCE(SUM(d.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),COALESCE(SUM(d.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(d.thuNhapThuan),0) " +
           "FROM DuLieuMpa d WHERE d.maDonViCap6=:maPhong AND d.thang=:thang AND d.nam=:nam AND d.loaiKy = 'NAM'")
    List<Object[]> phongBenchmarkByNamLuyKe(@Param("maPhong") String maPhong, @Param("thang") int thang, @Param("nam") int nam);

    // ── Trend per CIF ─────────────────────────────────────────────────

    @Query("SELECT d.thang," +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0),COALESCE(SUM(d.huyDongVonBinhQuan),0)," +
           "COALESCE(SUM(d.duNoTinDungCuoiKy),0),COALESCE(SUM(d.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(d.thuNhapThuanHdvFtp),0),COALESCE(SUM(d.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(d.thuNhapThuan),0) " +
           "FROM DuLieuMpa d WHERE d.maKhCif=:cif AND d.nam=:nam AND d.loaiKy = 'THANG' " +
           "GROUP BY d.thang ORDER BY d.thang")
    List<Object[]> cifTrendByNam(@Param("cif") String cif, @Param("nam") int nam);

    // ── Customer info fallback from du_lieu_mpa ────────────────────────

    @Query("SELECT MIN(d.tenKhachHang),MIN(d.maDonViCap6),MIN(d.tenDonViCap6),MIN(d.maAm),MIN(d.tenAm) " +
           "FROM DuLieuMpa d WHERE d.maKhCif=:cif")
    List<Object[]> cifInfoFallback(@Param("cif") String cif);

    // ── Phong data – GROUP BY don_vi_cap_6 ────────────────────────
    // Object[]: [ma, ten, tnt, duNo, hdvCuoiKy, hdvBinhQuan, tntDichVu, tntHdv, tntTinDung, soKh, soAm]

    @Query("SELECT d.maDonViCap6, d.tenDonViCap6, " +
           "COALESCE(SUM(d.thuNhapThuan),0), COALESCE(SUM(d.duNoTinDungCuoiKy),0), " +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0), COALESCE(SUM(d.huyDongVonBinhQuan),0), " +
           "COALESCE(SUM(d.thuNhapThuanDichVu),0), COALESCE(SUM(d.thuNhapThuanHdvFtp),0), " +
           "COALESCE(SUM(d.thuNhapThuanTinDung),0), " +
           "COUNT(DISTINCT d.maKhCif), COUNT(DISTINCT d.maAm) " +
           "FROM DuLieuMpa d WHERE d.thang = :thang AND d.nam = :nam AND d.loaiKy = 'THANG' " +
           "GROUP BY d.maDonViCap6, d.tenDonViCap6 ORDER BY SUM(d.thuNhapThuan) DESC")
    List<Object[]> phongByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT d.maDonViCap6, d.tenDonViCap6, " +
           "COALESCE(SUM(d.thuNhapThuan),0), COALESCE(SUM(d.duNoTinDungCuoiKy),0), " +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0), COALESCE(SUM(d.huyDongVonBinhQuan),0), " +
           "COALESCE(SUM(d.thuNhapThuanDichVu),0), COALESCE(SUM(d.thuNhapThuanHdvFtp),0), " +
           "COALESCE(SUM(d.thuNhapThuanTinDung),0), " +
           "COUNT(DISTINCT d.maKhCif), COUNT(DISTINCT d.maAm) " +
           "FROM DuLieuMpa d WHERE d.quy = :quy AND d.nam = :nam AND d.loaiKy = 'QUY' " +
           "GROUP BY d.maDonViCap6, d.tenDonViCap6 ORDER BY SUM(d.thuNhapThuan) DESC")
    List<Object[]> phongByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT d.maDonViCap6, d.tenDonViCap6, " +
           "COALESCE(SUM(d.thuNhapThuan),0), COALESCE(SUM(d.duNoTinDungCuoiKy),0), " +
           "COALESCE(SUM(d.huyDongVonCuoiKy),0), COALESCE(SUM(d.huyDongVonBinhQuan),0), " +
           "COALESCE(SUM(d.thuNhapThuanDichVu),0), COALESCE(SUM(d.thuNhapThuanHdvFtp),0), " +
           "COALESCE(SUM(d.thuNhapThuanTinDung),0), " +
           "COUNT(DISTINCT d.maKhCif), COUNT(DISTINCT d.maAm) " +
           "FROM DuLieuMpa d WHERE d.thang = :thang AND d.nam = :nam AND d.loaiKy = 'NAM' " +
           "GROUP BY d.maDonViCap6, d.tenDonViCap6 ORDER BY SUM(d.thuNhapThuan) DESC")
    List<Object[]> phongByNamLuyKe(@Param("thang") int thang, @Param("nam") int nam);

    // ── DROPDOWN lists ─────────────────────────────────────────────

    @Query("SELECT DISTINCT d.maDonViCap6, d.tenDonViCap6 FROM DuLieuMpa d " +
           "WHERE d.maDonViCap6 IS NOT NULL ORDER BY d.tenDonViCap6")
    List<Object[]> findDistinctPhongList();

    @Query("SELECT DISTINCT d.tenPhanKhucKhCap2 FROM DuLieuMpa d " +
           "WHERE d.tenPhanKhucKhCap2 IS NOT NULL ORDER BY d.tenPhanKhucKhCap2")
    List<String> findDistinctPhanKhucList();

    // ── Danh sách có phân trang (menu "Dữ liệu MPA") ────────────────
    // thang/quy/nam/ngay đều optional — với loai_ky='NAM', thang chính là mốc lũy kế
    // (không phải kỳ đóng cố định), cần luôn truyền để tránh lẫn nhiều mốc cùng năm.
    @Query("""
        SELECT d FROM DuLieuMpa d
        WHERE d.loaiKy = :loaiKy
          AND (:thang IS NULL OR d.thang = :thang)
          AND (:quy IS NULL OR d.quy = :quy)
          AND (:nam IS NULL OR d.nam = :nam)
          AND (:ngay IS NULL OR d.ngay = :ngay)
          AND (:maDonViCap6 IS NULL OR d.maDonViCap6 = :maDonViCap6)
          AND (:search = ''
              OR LOWER(COALESCE(d.tenKhachHang,'')) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(COALESCE(d.tenAm,''))        LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(COALESCE(d.maKhCif,''))      LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(COALESCE(d.maAm,''))         LIKE LOWER(CONCAT('%', :search, '%'))
          )
        ORDER BY d.id DESC
        """)
    Page<DuLieuMpa> search(
            @Param("loaiKy") String loaiKy,
            @Param("thang") Integer thang,
            @Param("quy") String quy,
            @Param("nam") Integer nam,
            @Param("ngay") LocalDate ngay,
            @Param("maDonViCap6") String maDonViCap6,
            @Param("search") String search,
            Pageable pageable);

    // ── Tổng hợp (KPI cards) — cùng điều kiện lọc với search(), nhưng tính trên
    // TOÀN BỘ tập kết quả đã lọc, không chỉ trang hiện tại. Số khách hàng = COUNT DISTINCT CIF.
    @Query("""
        SELECT new com.mpa.dto.DuLieuMpaSummary(
            COUNT(DISTINCT d.maKhCif), COALESCE(SUM(d.thuNhapThuan),0), COALESCE(SUM(d.duNoTinDungCuoiKy),0))
        FROM DuLieuMpa d
        WHERE d.loaiKy = :loaiKy
          AND (:thang IS NULL OR d.thang = :thang)
          AND (:quy IS NULL OR d.quy = :quy)
          AND (:nam IS NULL OR d.nam = :nam)
          AND (:ngay IS NULL OR d.ngay = :ngay)
          AND (:maDonViCap6 IS NULL OR d.maDonViCap6 = :maDonViCap6)
          AND (:search = ''
              OR LOWER(COALESCE(d.tenKhachHang,'')) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(COALESCE(d.tenAm,''))        LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(COALESCE(d.maKhCif,''))      LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(COALESCE(d.maAm,''))         LIKE LOWER(CONCAT('%', :search, '%'))
          )
        """)
    DuLieuMpaSummary getSummary(
            @Param("loaiKy") String loaiKy,
            @Param("thang") Integer thang,
            @Param("quy") String quy,
            @Param("nam") Integer nam,
            @Param("ngay") LocalDate ngay,
            @Param("maDonViCap6") String maDonViCap6,
            @Param("search") String search);
}
