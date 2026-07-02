package com.mpa.repository;

import com.mpa.entity.ChiTieuBscChiNhanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

// Object[] projection for all queries:
// [0] unitKey, [1] unitName, [2] hdvCuoiKy, [3] casaBinhQuan, [4] duNo,
// [5] tntDichVu, [6] tntHdvFtp, [7] tntTinDung, [8] thuNhapThuan

public interface ChiTieuBscRepository extends JpaRepository<ChiTieuBscChiNhanh, Integer> {

    // ── KẾ HOẠCH (type_data = 0) ──────────────────────────────────

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6")
    List<Object[]> keHoachPhongByNam(@Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "GROUP BY c.maCn, c.tenCn")
    List<Object[]> keHoachCnByNam(@Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> keHoachAmByNam(@Param("nam") int nam);

    // ── THỰC HIỆN THEO THÁNG (type_data = 1) ────────────────────────

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6 ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> thucHienPhongByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.maCn, c.tenCn ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> thucHienCnByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> thucHienAmByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    // ── THỰC HIỆN THEO QUÝ (type_data = 5) ─────────────────────────

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6 ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> thucHienPhongByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam " +
           "GROUP BY c.maCn, c.tenCn ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> thucHienCnByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> thucHienAmByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    // ── THỰC HIỆN THEO NĂM (type_data = 6) ─────────────────────────

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 6 AND c.nam = :nam " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6 ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> thucHienPhongByNam(@Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 6 AND c.nam = :nam " +
           "GROUP BY c.maCn, c.tenCn ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> thucHienCnByNam(@Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 6 AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> thucHienAmByNam(@Param("nam") int nam);

    // ── QUẢN LÝ: full entity list (type_data = 0) ──────────────────

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "AND c.maDonViCap6 IS NOT NULL ORDER BY c.tenDonViCap6")
    List<ChiTieuBscChiNhanh> findKeHoachPhongList(@Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "AND c.maCn IS NOT NULL ORDER BY c.tenCn")
    List<ChiTieuBscChiNhanh> findKeHoachCnList(@Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "AND c.maAm IS NOT NULL ORDER BY c.tenAm")
    List<ChiTieuBscChiNhanh> findKeHoachAmList(@Param("nam") int nam);

    // ── UPSERT: find existing record for a specific unit ───────────

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "AND c.maDonViCap6 = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachPhong(@Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "AND c.maCn = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachCn(@Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "AND c.maAm = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachAm(@Param("nam") int nam, @Param("ma") String ma);

    // ── DROPDOWN: distinct chi nhánh list ─────────────────────────

    @Query("SELECT DISTINCT c.maCn, c.tenCn FROM ChiTieuBscChiNhanh c " +
           "WHERE c.maCn IS NOT NULL ORDER BY c.tenCn")
    List<Object[]> findDistinctCnList();
}
