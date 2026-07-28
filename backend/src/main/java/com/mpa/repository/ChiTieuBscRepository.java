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

    // ── KẾ HOẠCH THEO NĂM (type_data = 0) ──────────────────────────

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam AND c.maDonViCap6 IS NOT NULL " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6")
    List<Object[]> keHoachPhongByNam(@Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam AND c.maCn IS NOT NULL " +
           "GROUP BY c.maCn, c.tenCn")
    List<Object[]> keHoachCnByNam(@Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam AND c.maAm IS NOT NULL " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> keHoachAmByNam(@Param("nam") int nam);

    // ── KẾ HOẠCH THEO THÁNG (type_data = 1) ─────────────────────────

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam AND c.maDonViCap6 IS NOT NULL " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6")
    List<Object[]> keHoachPhongByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam AND c.maCn IS NOT NULL " +
           "GROUP BY c.maCn, c.tenCn")
    List<Object[]> keHoachCnByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam AND c.maAm IS NOT NULL " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> keHoachAmByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    // ── KẾ HOẠCH THEO QUÝ (type_data = 5) ───────────────────────────

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam AND c.maDonViCap6 IS NOT NULL " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6")
    List<Object[]> keHoachPhongByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam AND c.maCn IS NOT NULL " +
           "GROUP BY c.maCn, c.tenCn")
    List<Object[]> keHoachCnByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam AND c.maAm IS NOT NULL " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> keHoachAmByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    // "THỰC HIỆN" đọc từ bảng thuc_hien_bsc_chi_nhanh (ThucHienBscRepository), KHÔNG đọc từ
    // chi_tieu_bsc_chi_nhanh — bảng này chỉ chứa kế hoạch (type_data=0/1/5, xem trên).

    // ── XU HƯỚNG: kế hoạch gộp theo cả năm (dùng cho tab "Xu hướng") ──
    // Shape: [period, hdvCuoiKy, casaBinhQuan, duNo, tntDichVu, tntHdvFtp, tntTinDung, tnt]

    @Query("SELECT c.thang, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.nam = :nam AND c.maCn IS NOT NULL " +
           "GROUP BY c.thang ORDER BY c.thang")
    List<Object[]> keHoachTrendCnByThang(@Param("nam") int nam);

    @Query("SELECT c.thang, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.nam = :nam AND c.maDonViCap6 = :ma " +
           "GROUP BY c.thang ORDER BY c.thang")
    List<Object[]> keHoachTrendPhongByThang(@Param("ma") String ma, @Param("nam") int nam);

    @Query("SELECT c.thang, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.nam = :nam AND c.maAm = :ma " +
           "GROUP BY c.thang ORDER BY c.thang")
    List<Object[]> keHoachTrendAmByThang(@Param("ma") String ma, @Param("nam") int nam);

    @Query("SELECT c.quy, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.nam = :nam AND c.maCn IS NOT NULL " +
           "GROUP BY c.quy ORDER BY c.quy")
    List<Object[]> keHoachTrendCnByQuy(@Param("nam") int nam);

    @Query("SELECT c.quy, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.nam = :nam AND c.maDonViCap6 = :ma " +
           "GROUP BY c.quy ORDER BY c.quy")
    List<Object[]> keHoachTrendPhongByQuy(@Param("ma") String ma, @Param("nam") int nam);

    @Query("SELECT c.quy, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.nam = :nam AND c.maAm = :ma " +
           "GROUP BY c.quy ORDER BY c.quy")
    List<Object[]> keHoachTrendAmByQuy(@Param("ma") String ma, @Param("nam") int nam);

    @Query("SELECT c.nam, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam IN :years AND c.maCn IS NOT NULL " +
           "GROUP BY c.nam ORDER BY c.nam")
    List<Object[]> keHoachTrendCnByNam(@Param("years") List<Integer> years);

    @Query("SELECT c.nam, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam IN :years AND c.maDonViCap6 = :ma " +
           "GROUP BY c.nam ORDER BY c.nam")
    List<Object[]> keHoachTrendPhongByNam(@Param("ma") String ma, @Param("years") List<Integer> years);

    @Query("SELECT c.nam, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam IN :years AND c.maAm = :ma " +
           "GROUP BY c.nam ORDER BY c.nam")
    List<Object[]> keHoachTrendAmByNam(@Param("ma") String ma, @Param("years") List<Integer> years);

    // ── XU HƯỚNG (nhiều mã AM cùng lúc — dùng cho AM detail / Cán bộ gộp) ──
    // Cùng shape với keHoachTrendAmBy{Thang,Quy,Nam} ở trên, chỉ khác IN thay vì =.
    // Danh sách 1 phần tử cho ra kết quả giống hệt bản = tương ứng.

    @Query("SELECT c.thang, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.nam = :nam AND c.maAm IN :maAmCodes " +
           "GROUP BY c.thang ORDER BY c.thang")
    List<Object[]> keHoachTrendAmListByThang(@Param("maAmCodes") List<String> maAmCodes, @Param("nam") int nam);

    @Query("SELECT c.quy, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.nam = :nam AND c.maAm IN :maAmCodes " +
           "GROUP BY c.quy ORDER BY c.quy")
    List<Object[]> keHoachTrendAmListByQuy(@Param("maAmCodes") List<String> maAmCodes, @Param("nam") int nam);

    @Query("SELECT c.nam, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam IN :years AND c.maAm IN :maAmCodes " +
           "GROUP BY c.nam ORDER BY c.nam")
    List<Object[]> keHoachTrendAmListByNam(@Param("maAmCodes") List<String> maAmCodes, @Param("years") List<Integer> years);

    // ── QUẢN LÝ: full entity list theo kỳ (kế hoạch) ────────────────

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "AND c.maDonViCap6 IS NOT NULL ORDER BY c.tenDonViCap6")
    List<ChiTieuBscChiNhanh> findKeHoachPhongListNam(@Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "AND c.maCn IS NOT NULL ORDER BY c.tenCn")
    List<ChiTieuBscChiNhanh> findKeHoachCnListNam(@Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam " +
           "AND c.maAm IS NOT NULL ORDER BY c.tenAm")
    List<ChiTieuBscChiNhanh> findKeHoachAmListNam(@Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam " +
           "AND c.maDonViCap6 IS NOT NULL ORDER BY c.tenDonViCap6")
    List<ChiTieuBscChiNhanh> findKeHoachPhongListThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam " +
           "AND c.maCn IS NOT NULL ORDER BY c.tenCn")
    List<ChiTieuBscChiNhanh> findKeHoachCnListThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam " +
           "AND c.maAm IS NOT NULL ORDER BY c.tenAm")
    List<ChiTieuBscChiNhanh> findKeHoachAmListThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam " +
           "AND c.maDonViCap6 IS NOT NULL ORDER BY c.tenDonViCap6")
    List<ChiTieuBscChiNhanh> findKeHoachPhongListQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam " +
           "AND c.maCn IS NOT NULL ORDER BY c.tenCn")
    List<ChiTieuBscChiNhanh> findKeHoachCnListQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam " +
           "AND c.maAm IS NOT NULL ORDER BY c.tenAm")
    List<ChiTieuBscChiNhanh> findKeHoachAmListQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    // ── UPSERT: tìm bản ghi kế hoạch đã có của đúng kỳ + đơn vị ─────

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam AND c.maDonViCap6 = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachPhongNam(@Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam AND c.maCn = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachCnNam(@Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 0 AND c.nam = :nam AND c.maAm = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachAmNam(@Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam AND c.maDonViCap6 = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachPhongThangNam(@Param("thang") int thang, @Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam AND c.maCn = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachCnThangNam(@Param("thang") int thang, @Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam AND c.maAm = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachAmThangNam(@Param("thang") int thang, @Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam AND c.maDonViCap6 = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachPhongQuyNam(@Param("quy") String quy, @Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam AND c.maCn = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachCnQuyNam(@Param("quy") String quy, @Param("nam") int nam, @Param("ma") String ma);

    @Query("SELECT c FROM ChiTieuBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam AND c.maAm = :ma")
    Optional<ChiTieuBscChiNhanh> findKeHoachAmQuyNam(@Param("quy") String quy, @Param("nam") int nam, @Param("ma") String ma);

    // ── DROPDOWN: distinct chi nhánh list ─────────────────────────

    @Query("SELECT DISTINCT c.maCn, c.tenCn FROM ChiTieuBscChiNhanh c " +
           "WHERE c.maCn IS NOT NULL ORDER BY c.tenCn")
    List<Object[]> findDistinctCnList();
}
