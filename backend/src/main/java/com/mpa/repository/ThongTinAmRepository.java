package com.mpa.repository;

import com.mpa.entity.ThongTinAm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
