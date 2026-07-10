package com.mpa.repository;

import com.mpa.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    // Chỉ dùng SUCCESS làm mốc dedup — SKIPPED_DISABLED/FAILED nghĩa là KH chưa thực sự nhận được
    // email, nên không được tính là "đã gửi" (nếu không, bật CARD_EMAIL_ENABLED lên sau đó sẽ
    // không bao giờ gửi lại được cho tới khi hết chu kỳ lặp lại).
    //
    // Các method dưới đây fetch HÀNG LOẠT (1 query cho cả job) thay vì 1 query/thẻ — bắt buộc
    // với các job/chiến dịch có thể match hàng chục nghìn thẻ (xem SnapshotServiceImpl cho vấn
    // đề tương tự đã gặp phải với upsert từng dòng).
    List<EmailLog> findByLoaiThongBaoAndTrangThaiAndNgayGuiAfter(
            String loaiThongBao, String trangThai, LocalDateTime after);

    List<EmailLog> findByCampaignIdAndTrangThai(Integer campaignId, String trangThai);

    List<EmailLog> findByMilestoneIdAndTrangThai(Integer milestoneId, String trangThai);

    @Query("""
        SELECT e FROM EmailLog e
        WHERE (:cardId IS NULL OR e.cardId = :cardId)
          AND (:loaiThongBao IS NULL OR e.loaiThongBao = :loaiThongBao)
          AND (:campaignId IS NULL OR e.campaignId = :campaignId)
        ORDER BY e.ngayGui DESC
        """)
    Page<EmailLog> search(@Param("cardId") String cardId,
                           @Param("loaiThongBao") String loaiThongBao,
                           @Param("campaignId") Integer campaignId,
                           Pageable pageable);
}
