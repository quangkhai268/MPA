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

    /**
     * 7 bản ghi snapshot mới nhất của 1 thẻ (mới nhất trước) — dùng cho chart "Theo ngày"
     * ở trang chi tiết thẻ. Không lọc theo khoảng ngày vì snapshot giờ chỉ tạo khi có import
     * (không chạy hằng ngày), các lần snapshot có thể cách nhau nhiều ngày — cần lấy đúng
     * 7 LẦN gần nhất chứ không phải 7 ngày dương lịch gần nhất.
     */
    List<TheDoanhSoSnapshot> findFirst7ByCardIdOrderByNgaySnapshotDesc(String cardId);
}
