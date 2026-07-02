package com.mpa.repository;

import com.mpa.entity.PhongBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PhongBanRepository extends JpaRepository<PhongBan, Integer> {

    @Query("SELECT p FROM PhongBan p WHERE p.trangThai = 1 ORDER BY p.tenDonViCap6")
    List<PhongBan> findAllActive();
}
