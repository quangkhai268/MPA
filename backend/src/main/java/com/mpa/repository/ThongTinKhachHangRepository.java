package com.mpa.repository;

import com.mpa.entity.ThongTinKhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThongTinKhachHangRepository extends JpaRepository<ThongTinKhachHang, Integer> {

    // search: khớp CIF/tên/SĐT/email · maDonViCap6: lọc theo Phòng · amSearch: khớp mã hoặc tên AM
    // (subquery sang ThongTinAm vì ThongTinKhachHang chỉ lưu ma_am, không lưu tên) · phanKhuc:
    // lọc theo ten_phan_khuc_kh_cap_2 thực tế của khách hàng, lấy từ du_lieu_mpa (không phải
    // type_khach_hang — cột đó vẫn dùng riêng cho badge Phân khúc + benchmark chi tiết KH).
    @Query("""
        SELECT t FROM ThongTinKhachHang t
        WHERE ('' = :search
            OR LOWER(t.maKhCif) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.tenKhachHang) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(t.soDienThoai,'')) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(COALESCE(t.email,'')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:maDonViCap6 IS NULL OR t.maDonViCap6 = :maDonViCap6)
          AND (:amSearch = ''
              OR LOWER(COALESCE(t.maAm,'')) LIKE LOWER(CONCAT('%', :amSearch, '%'))
              OR t.maAm IN (SELECT a.maAm FROM ThongTinAm a WHERE LOWER(a.tenAm) LIKE LOWER(CONCAT('%', :amSearch, '%')))
          )
          AND (:phanKhuc = ''
              OR t.maKhCif IN (SELECT DISTINCT d.maKhCif FROM DuLieuMpa d WHERE d.tenPhanKhucKhCap2 = :phanKhuc)
          )
        ORDER BY t.tenKhachHang
        """)
    Page<ThongTinKhachHang> search(
            @Param("search") String search,
            @Param("maDonViCap6") String maDonViCap6,
            @Param("amSearch") String amSearch,
            @Param("phanKhuc") String phanKhuc,
            Pageable pageable);

    // 1 CIF có thể có nhiều dòng (gắn nhiều AM/phòng khác nhau theo thời gian) — lấy dòng mới nhất làm đại diện.
    Optional<ThongTinKhachHang> findFirstByMaKhCifOrderByIdDesc(String maKhCif);

    @Query("SELECT t.maKhCif FROM ThongTinKhachHang t WHERE t.typeKhachHang = :type AND t.trangThai = 1")
    List<String> findCifsByType(@Param("type") Integer type);
}
