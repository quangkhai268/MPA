package com.mpa.repository;

import com.mpa.dto.KpiSumResult;
import com.mpa.entity.ThucHienBscChiNhanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ThucHienBscRepository extends JpaRepository<ThucHienBscChiNhanh, Integer> {

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(c.huyDongVonCuoiKy), 0), " +
           "COALESCE(SUM(c.casaBinhQuan), 0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy), 0), " +
           "COALESCE(SUM(c.thuNhapThuanDichVu), 0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp), 0), " +
           "COALESCE(SUM(c.thuNhapThuanTinDung), 0), " +
           "COALESCE(SUM(c.thuNhapThuan), 0)) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam")
    KpiSumResult sumThucHienByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(c.huyDongVonCuoiKy), 0), " +
           "COALESCE(SUM(c.casaBinhQuan), 0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy), 0), " +
           "COALESCE(SUM(c.thuNhapThuanDichVu), 0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp), 0), " +
           "COALESCE(SUM(c.thuNhapThuanTinDung), 0), " +
           "COALESCE(SUM(c.thuNhapThuan), 0)) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam")
    KpiSumResult sumThucHienByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(c.huyDongVonCuoiKy), 0), " +
           "COALESCE(SUM(c.casaBinhQuan), 0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy), 0), " +
           "COALESCE(SUM(c.thuNhapThuanDichVu), 0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp), 0), " +
           "COALESCE(SUM(c.thuNhapThuanTinDung), 0), " +
           "COALESCE(SUM(c.thuNhapThuan), 0)) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 6 AND c.nam = :nam")
    KpiSumResult sumThucHienByNam(@Param("nam") int nam);

    @Query("SELECT new com.mpa.dto.KpiSumResult(" +
           "COALESCE(SUM(c.huyDongVonCuoiKy), 0), " +
           "COALESCE(SUM(c.casaBinhQuan), 0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy), 0), " +
           "COALESCE(SUM(c.thuNhapThuanDichVu), 0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp), 0), " +
           "COALESCE(SUM(c.thuNhapThuanTinDung), 0), " +
           "COALESCE(SUM(c.thuNhapThuan), 0)) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 1 AND c.ngay = :ngay")
    KpiSumResult sumThucHienByNgay(@Param("ngay") java.time.LocalDate ngay);

    // type_data=0 (kế hoạch/chỉ tiêu) không còn dùng ở bảng này — menu Giao chỉ tiêu ghi
    // vào chi_tieu_bsc_chi_nhanh (xem ChiTieuBscRepository.keHoachCnByNam), Dashboard đọc
    // chỉ tiêu từ đó (DashboardServiceImpl.khTuChiTieu).

    // ── Trend queries – trả về Object[] [period, hdv, casa, duno, tntdv, tnthdv, tnttd, tnt]
    @Query("SELECT c.thang, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 1 AND c.nam = :nam " +
           "GROUP BY c.thang ORDER BY c.thang")
    List<Object[]> trendAllByThang(@Param("nam") int nam);

    @Query("SELECT c.quy, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 5 AND c.nam = :nam " +
           "GROUP BY c.quy ORDER BY c.quy")
    List<Object[]> trendAllByQuy(@Param("nam") int nam);

    @Query("SELECT c.nam, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 6 AND c.nam IN :years " +
           "GROUP BY c.nam ORDER BY c.nam")
    List<Object[]> trendAllByNam(@Param("years") List<Integer> years);

    @Query("SELECT c.ngay, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.ngay ORDER BY c.ngay")
    List<Object[]> trendAllByNgay(@Param("thang") int thang, @Param("nam") int nam);

    // ── Phong table queries (type_data=2, per-period snapshot) ────
    // Object[]: [maDonViCap6, tenDonViCap6, hdvCuoiKy, casa, duNo, tntDv, tntHdv, tntTd, tnt]
    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6 ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> phongTableByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.quy = :quy AND c.nam = :nam " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6 ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> phongTableByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.nam = :nam " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6 ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> phongTableByNam(@Param("nam") int nam);

    // ── Phong AM count (type_data=3) – Object[]: [tenDonViCap6, soAm]
    // Join bằng tenDonViCap6 vì maDonViCap6 không nhất quán giữa type_data=2 và type_data=3
    @Query("SELECT c.tenDonViCap6, COUNT(c) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.tenDonViCap6")
    List<Object[]> phongAmCountByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c.tenDonViCap6, COUNT(c) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.quy = :quy AND c.nam = :nam " +
           "GROUP BY c.tenDonViCap6")
    List<Object[]> phongAmCountByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c.tenDonViCap6, COUNT(c) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.nam = :nam " +
           "GROUP BY c.tenDonViCap6")
    List<Object[]> phongAmCountByNam(@Param("nam") int nam);

    // ── AM detail queries (type_data=3) – Object[]: [maAm, tenAm, hdvCuoiKy, casa, duNo, tntDv, tntHdv, tntTd, tnt]
    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c " +
           "WHERE c.typeData = 3 AND c.maDonViCap6 = :maDonViCap6 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> amDetailByThangNam(@Param("maDonViCap6") String maDonViCap6,
                                       @Param("thang") int thang,
                                       @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c " +
           "WHERE c.typeData = 3 AND c.maDonViCap6 = :maDonViCap6 AND c.quy = :quy AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> amDetailByQuyNam(@Param("maDonViCap6") String maDonViCap6,
                                     @Param("quy") String quy,
                                     @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c " +
           "WHERE c.typeData = 3 AND c.maDonViCap6 = :maDonViCap6 AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> amDetailByNam(@Param("maDonViCap6") String maDonViCap6,
                                  @Param("nam") int nam);

    // ── Top AM toàn chi nhánh (type_data=3, không lọc theo phòng) ──
    // Object[]: [maAm, tenAm, tenDonViCap6, hdvCuoiKy, casaBinhQuan, duNo, tntDichVu, tntHdvFtp, tntTinDung, tnt]
    // (đủ 7 chỉ tiêu để chọn xếp hạng theo bất kỳ chỉ tiêu nào — sắp xếp thực hiện ở tầng Java theo metricKey)
    @Query("SELECT c.maAm, c.tenAm, MAX(c.tenDonViCap6), " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), COALESCE(SUM(c.duNoTinDungCuoiKy),0), " +
           "COALESCE(SUM(c.thuNhapThuanDichVu),0), COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> topAmByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, MAX(c.tenDonViCap6), " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), COALESCE(SUM(c.duNoTinDungCuoiKy),0), " +
           "COALESCE(SUM(c.thuNhapThuanDichVu),0), COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.quy = :quy AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> topAmByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, MAX(c.tenDonViCap6), " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), COALESCE(SUM(c.duNoTinDungCuoiKy),0), " +
           "COALESCE(SUM(c.thuNhapThuanDichVu),0), COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> topAmByNam(@Param("nam") int nam);

    // ── Phong trend queries (type_data=2) – [tenDonViCap6, period, hdv, casa, duno, tntdv, tnthdv, tnttd, tnt]
    @Query("SELECT c.tenDonViCap6, c.thang, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.nam = :nam AND c.thang IS NOT NULL " +
           "GROUP BY c.tenDonViCap6, c.thang ORDER BY c.tenDonViCap6, c.thang")
    List<Object[]> phongTrendByThang(@Param("nam") int nam);

    @Query("SELECT c.tenDonViCap6, c.quy, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.nam = :nam AND c.quy IS NOT NULL " +
           "GROUP BY c.tenDonViCap6, c.quy ORDER BY c.tenDonViCap6, c.quy")
    List<Object[]> phongTrendByQuy(@Param("nam") int nam);

    @Query("SELECT c.tenDonViCap6, c.nam, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.nam IN :years " +
           "GROUP BY c.tenDonViCap6, c.nam ORDER BY c.tenDonViCap6, c.nam")
    List<Object[]> phongTrendByNam(@Param("years") List<Integer> years);

    // ── Dùng cho "Dashboard so sánh" (giao-chi-tieu): thực hiện theo tổng chi nhánh / AM,
    // hình dạng Object[] thống nhất [key, name, hdv, casa, duNo, tntDv, tntHdv, tntTd, tnt]
    // (9 cột — khớp với phongTableByX ở trên) để tái dùng chung 1 pipeline gộp/so sánh.

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 1 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.maCn, c.tenCn")
    List<Object[]> cnRowByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 5 AND c.quy = :quy AND c.nam = :nam " +
           "GROUP BY c.maCn, c.tenCn")
    List<Object[]> cnRowByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 6 AND c.nam = :nam " +
           "GROUP BY c.maCn, c.tenCn")
    List<Object[]> cnRowByNam(@Param("nam") int nam);

    @Query("SELECT c.maCn, c.tenCn, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 1 AND c.ngay = " +
           "(SELECT MAX(c2.ngay) FROM ThucHienBscChiNhanh c2 WHERE c2.typeData = 1 AND c2.ngay IS NOT NULL) " +
           "GROUP BY c.maCn, c.tenCn")
    List<Object[]> cnRowByNgayLatest();

    @Query("SELECT c.maDonViCap6, c.tenDonViCap6, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.ngay = " +
           "(SELECT MAX(c2.ngay) FROM ThucHienBscChiNhanh c2 WHERE c2.typeData = 2 AND c2.ngay IS NOT NULL) " +
           "GROUP BY c.maDonViCap6, c.tenDonViCap6 ORDER BY SUM(c.thuNhapThuan) DESC")
    List<Object[]> phongTableByNgayLatest();

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.thang = :thang AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> amRowByThangNam(@Param("thang") int thang, @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.quy = :quy AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> amRowByQuyNam(@Param("quy") String quy, @Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.nam = :nam " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> amRowByNam(@Param("nam") int nam);

    @Query("SELECT c.maAm, c.tenAm, " +
           "COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.ngay = " +
           "(SELECT MAX(c2.ngay) FROM ThucHienBscChiNhanh c2 WHERE c2.typeData = 3 AND c2.ngay IS NOT NULL) " +
           "GROUP BY c.maAm, c.tenAm")
    List<Object[]> amRowByNgayLatest();

    @Query("SELECT MAX(c.ngay) FROM ThucHienBscChiNhanh c WHERE c.typeData = 1 AND c.ngay IS NOT NULL")
    java.time.LocalDate findLatestNgay();

    // ── XU HƯỚNG: thực hiện gộp theo cả năm, đúng 1 phòng / 1 AM cụ thể ──
    // (dùng cho tab "Xu hướng" — khác phongTrendByX ở trên vì đó liệt kê TẤT CẢ phòng,
    // còn đây chỉ lấy đúng 1 đơn vị). Shape: [period, hdv, casa, duno, tntdv, tnthdv, tnttd, tnt]

    @Query("SELECT c.thang, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.maDonViCap6 = :maDonViCap6 AND c.nam = :nam AND c.thang IS NOT NULL " +
           "GROUP BY c.thang ORDER BY c.thang")
    List<Object[]> trendPhongOneByThang(@Param("maDonViCap6") String maDonViCap6, @Param("nam") int nam);

    @Query("SELECT c.quy, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.maDonViCap6 = :maDonViCap6 AND c.nam = :nam AND c.quy IS NOT NULL " +
           "GROUP BY c.quy ORDER BY c.quy")
    List<Object[]> trendPhongOneByQuy(@Param("maDonViCap6") String maDonViCap6, @Param("nam") int nam);

    @Query("SELECT c.nam, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 2 AND c.maDonViCap6 = :maDonViCap6 AND c.nam IN :years " +
           "GROUP BY c.nam ORDER BY c.nam")
    List<Object[]> trendPhongOneByNam(@Param("maDonViCap6") String maDonViCap6, @Param("years") List<Integer> years);

    @Query("SELECT c.thang, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.maAm = :maAm AND c.nam = :nam AND c.thang IS NOT NULL " +
           "GROUP BY c.thang ORDER BY c.thang")
    List<Object[]> trendAmOneByThang(@Param("maAm") String maAm, @Param("nam") int nam);

    @Query("SELECT c.quy, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.maAm = :maAm AND c.nam = :nam AND c.quy IS NOT NULL " +
           "GROUP BY c.quy ORDER BY c.quy")
    List<Object[]> trendAmOneByQuy(@Param("maAm") String maAm, @Param("nam") int nam);

    @Query("SELECT c.nam, COALESCE(SUM(c.huyDongVonCuoiKy),0), COALESCE(SUM(c.casaBinhQuan),0), " +
           "COALESCE(SUM(c.duNoTinDungCuoiKy),0), COALESCE(SUM(c.thuNhapThuanDichVu),0), " +
           "COALESCE(SUM(c.thuNhapThuanHdvFtp),0), COALESCE(SUM(c.thuNhapThuanTinDung),0), " +
           "COALESCE(SUM(c.thuNhapThuan),0) " +
           "FROM ThucHienBscChiNhanh c WHERE c.typeData = 3 AND c.maAm = :maAm AND c.nam IN :years " +
           "GROUP BY c.nam ORDER BY c.nam")
    List<Object[]> trendAmOneByNam(@Param("maAm") String maAm, @Param("years") List<Integer> years);
}
