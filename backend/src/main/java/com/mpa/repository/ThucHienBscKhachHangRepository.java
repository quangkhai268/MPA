package com.mpa.repository;

import com.mpa.dto.KpiSumResult;
import com.mpa.entity.ThucHienBscKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ThucHienBscKhachHangRepository extends JpaRepository<ThucHienBscKhachHang, Integer> {

    // ── Sum for a single CIF ─────────────────────────────────────────────

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(c.huyDongVonCuoiKy), 0), COALESCE(SUM(c.casaBinhQuan), 0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy), 0), COALESCE(SUM(c.thuNhapThuanDichVu), 0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp), 0), COALESCE(SUM(c.thuNhapThuanTinDung), 0), " +
           "COALESCE(SUM(c.thuNhapThuan), 0)) " +
           "FROM ThucHienBscKhachHang c WHERE c.maKhCif = :cif AND c.typeData = 1 AND c.thang = :thang AND c.nam = :nam")
    KpiSumResult sumByCifAndThangNam(@Param("cif") String cif,
                                     @Param("thang") int thang,
                                     @Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(c.huyDongVonCuoiKy), 0), COALESCE(SUM(c.casaBinhQuan), 0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy), 0), COALESCE(SUM(c.thuNhapThuanDichVu), 0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp), 0), COALESCE(SUM(c.thuNhapThuanTinDung), 0), " +
           "COALESCE(SUM(c.thuNhapThuan), 0)) " +
           "FROM ThucHienBscKhachHang c WHERE c.maKhCif = :cif AND c.typeData = 5 AND c.quy = :quy AND c.nam = :nam")
    KpiSumResult sumByCifAndQuyNam(@Param("cif") String cif,
                                   @Param("quy") String quy,
                                   @Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(c.huyDongVonCuoiKy), 0), COALESCE(SUM(c.casaBinhQuan), 0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy), 0), COALESCE(SUM(c.thuNhapThuanDichVu), 0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp), 0), COALESCE(SUM(c.thuNhapThuanTinDung), 0), " +
           "COALESCE(SUM(c.thuNhapThuan), 0)) " +
           "FROM ThucHienBscKhachHang c WHERE c.maKhCif = :cif AND c.typeData = 6 AND c.nam = :nam")
    KpiSumResult sumByCifAndNam(@Param("cif") String cif, @Param("nam") int nam);

    // ── Benchmark: SUM + COUNT cho cùng phân khúc (dùng subquery thay IN list) ─
    // Object[]: [0]=count(distinct cif), [1..7]=SUM của 7 chỉ tiêu

    @Query("SELECT COUNT(DISTINCT c.maKhCif)," +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0),COALESCE(SUM(c.casaBinhQuan),0)," +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0),COALESCE(SUM(c.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0),COALESCE(SUM(c.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscKhachHang c " +
           "WHERE c.maKhCif IN (SELECT t.maKhCif FROM ThongTinKhachHang t WHERE t.typeKhachHang=:type) " +
           "AND c.typeData=1 AND c.thang=:thang AND c.nam=:nam")
    List<Object[]> benchmarkByTypeThangNam(@Param("type") int type,
                                           @Param("thang") int thang,
                                           @Param("nam") int nam);

    @Query("SELECT COUNT(DISTINCT c.maKhCif)," +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0),COALESCE(SUM(c.casaBinhQuan),0)," +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0),COALESCE(SUM(c.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0),COALESCE(SUM(c.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscKhachHang c " +
           "WHERE c.maKhCif IN (SELECT t.maKhCif FROM ThongTinKhachHang t WHERE t.typeKhachHang=:type) " +
           "AND c.typeData=5 AND c.quy=:quy AND c.nam=:nam")
    List<Object[]> benchmarkByTypeQuyNam(@Param("type") int type,
                                         @Param("quy") String quy,
                                         @Param("nam") int nam);

    @Query("SELECT COUNT(DISTINCT c.maKhCif)," +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0),COALESCE(SUM(c.casaBinhQuan),0)," +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0),COALESCE(SUM(c.thuNhapThuanDichVu),0)," +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0),COALESCE(SUM(c.thuNhapThuanTinDung),0)," +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscKhachHang c " +
           "WHERE c.maKhCif IN (SELECT t.maKhCif FROM ThongTinKhachHang t WHERE t.typeKhachHang=:type) " +
           "AND c.typeData=6 AND c.nam=:nam")
    List<Object[]> benchmarkByTypeNam(@Param("type") int type, @Param("nam") int nam);

    // ── Monthly trend (sparkline) ─────────────────────────────────────────
    // Object[]: [thang, hdvCuoiKy, casaBinhQuan, duNo, tntDv, tntHdv, tntTd, tnt]
    @Query("SELECT c.thang, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscKhachHang c WHERE c.maKhCif = :cif AND c.typeData = 1 AND c.nam = :nam " +
           "GROUP BY c.thang ORDER BY c.thang")
    List<Object[]> trendByThang(@Param("cif") String cif, @Param("nam") int nam);
}
