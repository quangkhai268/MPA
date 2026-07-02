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
        WHERE ('' = :search
            OR LOWER(t.maAm) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.tenAm) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.tenAm, t.maAm
        """)
    Page<ThongTinAm> searchAll(@Param("search") String search, Pageable pageable);

    @Query("""
        SELECT t FROM ThongTinAm t
        WHERE t.trangThai = :trangThai
        AND ('' = :search
            OR LOWER(t.maAm) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.tenAm) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.tenAm, t.maAm
        """)
    Page<ThongTinAm> searchByStatus(@Param("search") String search,
                                     @Param("trangThai") Short trangThai,
                                     Pageable pageable);

    @Query("""
        SELECT t FROM ThongTinAm t
        WHERE ('' = :search
            OR LOWER(t.maAm) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.tenAm) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.tenAm, t.maAm
        """)
    List<ThongTinAm> searchAllList(@Param("search") String search);

    @Query("""
        SELECT t FROM ThongTinAm t
        WHERE t.trangThai = :trangThai
        AND ('' = :search
            OR LOWER(t.maAm) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.tenAm) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.tenAm, t.maAm
        """)
    List<ThongTinAm> searchByStatusList(@Param("search") String search,
                                         @Param("trangThai") Short trangThai);
}
