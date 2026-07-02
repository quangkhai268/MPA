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

    @Query("""
        SELECT t FROM ThongTinKhachHang t
        WHERE ('' = :search
            OR LOWER(t.maKhCif) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.tenKhachHang) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.soDienThoai) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.tenKhachHang
        """)
    Page<ThongTinKhachHang> searchAll(@Param("search") String search, Pageable pageable);

    @Query("""
        SELECT t FROM ThongTinKhachHang t
        WHERE t.typeKhachHang = :typeKhachHang
        AND ('' = :search
            OR LOWER(t.maKhCif) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.tenKhachHang) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.soDienThoai) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.tenKhachHang
        """)
    Page<ThongTinKhachHang> searchByType(@Param("search") String search,
                                          @Param("typeKhachHang") Integer typeKhachHang,
                                          Pageable pageable);

    @Query("SELECT DISTINCT t.typeKhachHang FROM ThongTinKhachHang t WHERE t.typeKhachHang IS NOT NULL ORDER BY t.typeKhachHang")
    List<Integer> findDistinctTypes();

    Optional<ThongTinKhachHang> findByMaKhCif(String maKhCif);

    @Query("SELECT t.maKhCif FROM ThongTinKhachHang t WHERE t.typeKhachHang = :type AND t.trangThai = 1")
    List<String> findCifsByType(@Param("type") Integer type);
}
