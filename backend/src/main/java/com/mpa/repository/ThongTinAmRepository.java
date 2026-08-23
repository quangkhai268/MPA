package com.mpa.repository;

import com.mpa.entity.ThongTinAm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThongTinAmRepository extends JpaRepository<ThongTinAm, Integer> {

    @Query("""
        SELECT t FROM ThongTinAm t
        WHERE (:trangThai IS NULL OR t.trangThai = :trangThai)
          AND (:maDonViCap6 IS NULL OR t.maDonViCap6 = :maDonViCap6)
          AND ('' = :search
              OR LOWER(t.maAm) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.tenAm) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.tenAm, t.maAm
        """)
    Page<ThongTinAm> search(@Param("search") String search,
                             @Param("trangThai") Short trangThai,
                             @Param("maDonViCap6") String maDonViCap6,
                             Pageable pageable);

    @Query("""
        SELECT t FROM ThongTinAm t
        WHERE (:trangThai IS NULL OR t.trangThai = :trangThai)
          AND (:maDonViCap6 IS NULL OR t.maDonViCap6 = :maDonViCap6)
          AND ('' = :search
              OR LOWER(t.maAm) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.tenAm) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.tenAm, t.maAm
        """)
    List<ThongTinAm> searchList(@Param("search") String search,
                                 @Param("trangThai") Short trangThai,
                                 @Param("maDonViCap6") String maDonViCap6);

    // [0]=tổng, [1]=hoạt động (trangThai=1), [2]=không hoạt động (trangThai khác 1) —
    // tổng luôn = hoạt động + không hoạt động, không phụ thuộc giá trị trangThai cụ thể.
    @Query("""
        SELECT COUNT(t),
               SUM(CASE WHEN t.trangThai = 1 THEN 1 ELSE 0 END),
               SUM(CASE WHEN t.trangThai IS NULL OR t.trangThai <> 1 THEN 1 ELSE 0 END)
        FROM ThongTinAm t
        WHERE (:maDonViCap6 IS NULL OR t.maDonViCap6 = :maDonViCap6)
          AND ('' = :search
              OR LOWER(t.maAm) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(t.tenAm) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    List<Object[]> countByStatus(@Param("search") String search, @Param("maDonViCap6") String maDonViCap6);

    /** Toàn bộ mã AM của 1 cán bộ (khớp chính xác tên) — dùng cho màn hình chi tiết AM theo cán bộ. */
    List<ThongTinAm> findByTenAm(String tenAm);

    Optional<ThongTinAm> findByMaAm(String maAm);
}
