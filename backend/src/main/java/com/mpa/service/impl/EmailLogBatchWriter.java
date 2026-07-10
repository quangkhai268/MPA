package com.mpa.service.impl;

import com.mpa.entity.EmailLog;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.util.List;

/**
 * Ghi hàng loạt email_log qua JdbcTemplate.batchUpdate thay vì gọi EmailLogRepository.save()
 * từng dòng trong vòng lặp — cần thiết khi 1 job/chiến dịch có thể tạo ra hàng chục nghìn dòng
 * log cùng lúc (xem SnapshotServiceImpl cho vấn đề tương tự với bảng the_doanh_so_snapshot).
 */
@Component
@RequiredArgsConstructor
public class EmailLogBatchWriter {

    private static final String INSERT_SQL = """
        INSERT INTO email_log (card_id, loai_thong_bao, email_to, ngay_gui, trang_thai, loi_chi_tiet, campaign_id, milestone_id, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;

    public void insertAll(List<EmailLog> logs) {
        for (int start = 0; start < logs.size(); start += BATCH_SIZE) {
            List<EmailLog> chunk = logs.subList(start, Math.min(start + BATCH_SIZE, logs.size()));
            jdbcTemplate.batchUpdate(INSERT_SQL, chunk, chunk.size(), (ps, log) -> {
                ps.setString(1, log.getCardId());
                ps.setString(2, log.getLoaiThongBao());
                ps.setString(3, log.getEmailTo());
                ps.setObject(4, log.getNgayGui());
                ps.setString(5, log.getTrangThai());
                ps.setString(6, log.getLoiChiTiet());
                if (log.getCampaignId() != null) ps.setInt(7, log.getCampaignId()); else ps.setNull(7, Types.INTEGER);
                if (log.getMilestoneId() != null) ps.setInt(8, log.getMilestoneId()); else ps.setNull(8, Types.INTEGER);
                ps.setObject(9, log.getCreatedAt());
            });
        }
    }
}
