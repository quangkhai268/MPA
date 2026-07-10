package com.mpa.service.impl;

import com.mpa.entity.ThePhatHanh;
import com.mpa.repository.ThePhatHanhRepository;
import com.mpa.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SnapshotServiceImpl implements SnapshotService {

    private static final String UPSERT_SQL = """
        INSERT INTO the_doanh_so_snapshot (card_id, ngay_snapshot, doanh_so_luy_ke, so_ngay_tu_phat_hanh, created_at)
        VALUES (?, ?, ?, ?, now())
        ON CONFLICT (card_id, ngay_snapshot)
        DO UPDATE SET doanh_so_luy_ke = EXCLUDED.doanh_so_luy_ke, so_ngay_tu_phat_hanh = EXCLUDED.so_ngay_tu_phat_hanh
        """;
    private static final int BATCH_SIZE = 500;

    private final ThePhatHanhRepository thePhatHanhRepo;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public int runDailySnapshot() {
        LocalDate today = LocalDate.now();
        List<ThePhatHanh> all = thePhatHanhRepo.findAll();
        List<ThePhatHanh> valid = all.stream()
                .filter(c -> c.getCardId() != null && !c.getCardId().isBlank())
                .toList();

        for (int start = 0; start < valid.size(); start += BATCH_SIZE) {
            List<ThePhatHanh> chunk = valid.subList(start, Math.min(start + BATCH_SIZE, valid.size()));
            jdbcTemplate.batchUpdate(UPSERT_SQL, chunk, chunk.size(), (ps, card) -> {
                BigDecimal doanhSo = card.getDoanhSoGiaoDichMienPtn() != null ? card.getDoanhSoGiaoDichMienPtn() : BigDecimal.ZERO;
                Integer soNgayTuPhatHanh = card.getNgayPhatHanhThe() != null
                        ? (int) ChronoUnit.DAYS.between(card.getNgayPhatHanhThe().toLocalDate(), today)
                        : null;
                ps.setString(1, card.getCardId());
                ps.setObject(2, today);
                ps.setBigDecimal(3, doanhSo);
                if (soNgayTuPhatHanh != null) ps.setInt(4, soNgayTuPhatHanh);
                else ps.setNull(4, Types.INTEGER);
            });
        }
        return valid.size();
    }
}
