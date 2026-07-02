package com.mpa.repository;

import com.mpa.entity.ChiNhanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ChiNhanhRepository extends JpaRepository<ChiNhanh, Integer> {

    @Query("SELECT c FROM ChiNhanh c WHERE c.trangThai = 1 ORDER BY c.tenCn")
    List<ChiNhanh> findAllActive();
}
