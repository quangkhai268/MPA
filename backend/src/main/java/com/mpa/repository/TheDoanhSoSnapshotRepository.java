package com.mpa.repository;

import com.mpa.entity.TheDoanhSoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TheDoanhSoSnapshotRepository extends JpaRepository<TheDoanhSoSnapshot, Long> {

    // Upsert hằng loạt được thực hiện qua JdbcTemplate.batchUpdate trong SnapshotServiceImpl
    // (nhanh hơn nhiều so với gọi từng dòng qua repository khi có ~20k+ thẻ).

    List<TheDoanhSoSnapshot> findByCardIdAndNgaySnapshotBetweenOrderByNgaySnapshot(
            String cardId, LocalDate from, LocalDate to);

    List<TheDoanhSoSnapshot> findByNgaySnapshotBetweenOrderByNgaySnapshot(LocalDate from, LocalDate to);
}
