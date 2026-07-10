package com.mpa.service.impl;

import com.mpa.dto.JobRunResult;
import com.mpa.entity.CardRevenueMilestone;
import com.mpa.entity.EmailLog;
import com.mpa.entity.EmailTemplate;
import com.mpa.entity.ThePhatHanh;
import com.mpa.repository.CardRevenueMilestoneRepository;
import com.mpa.repository.EmailLogRepository;
import com.mpa.repository.ThePhatHanhRepository;
import com.mpa.service.CardMilestoneEvaluationService;
import com.mpa.service.EmailService;
import com.mpa.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardMilestoneEvaluationServiceImpl implements CardMilestoneEvaluationService {

    private static final String LOAI_DOANH_SO_MOC = "DOANH_SO_MOC";

    private final CardRevenueMilestoneRepository milestoneRepo;
    private final ThePhatHanhRepository thePhatHanhRepo;
    private final EmailLogRepository emailLogRepo;
    private final EmailLogBatchWriter emailLogBatchWriter;
    private final EmailTemplateService templateService;
    private final EmailService emailService;

    @Override
    @Transactional
    public JobRunResult evaluateAndNotify(boolean testMode) {
        List<CardRevenueMilestone> milestones = milestoneRepo.findByActiveTrue();
        EmailTemplate template = templateService.getByLoaiThongBao(LOAI_DOANH_SO_MOC);

        int eligible = 0, sent = 0, skippedDedup = 0, skippedDisabled = 0, failed = 0;
        List<EmailLog> logsToInsert = new ArrayList<>();

        for (CardRevenueMilestone milestone : milestones) {
            LocalDate targetDate = LocalDate.now().minusDays(milestone.getSoNgayTuPhatHanh());
            List<ThePhatHanh> candidates = thePhatHanhRepo.findByNgayPhatHanhDate(targetDate);

            // 1 query cho cả mốc thay vì 1 query/thẻ.
            Set<String> daGuiTheoMoc = emailLogRepo.findByMilestoneIdAndTrangThai(milestone.getId(), "SUCCESS")
                    .stream().map(EmailLog::getCardId).collect(Collectors.toCollection(HashSet::new));

            for (ThePhatHanh card : candidates) {
                BigDecimal doanhSo = card.getDoanhSoGiaoDichMienPtn() != null ? card.getDoanhSoGiaoDichMienPtn() : BigDecimal.ZERO;
                if (doanhSo.compareTo(milestone.getNguongDoanhSo()) >= 0) {
                    continue; // đã đạt ngưỡng, không cần nhắc
                }
                eligible++;

                boolean daGuiRoi = daGuiTheoMoc.contains(card.getCardId());
                if (daGuiRoi) {
                    skippedDedup++;
                    continue;
                }

                if (testMode) {
                    sent++;
                    continue;
                }

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("tenKhachHang", nvl(card.getTenChuTheChinh(), card.getHoTenKhachHangPht()));
                placeholders.put("soThe", nvl(card.getSoTheDaPhatHanh(), ""));
                placeholders.put("soNgay", String.valueOf(milestone.getSoNgayTuPhatHanh()));
                placeholders.put("doanhSoHienTai", doanhSo.toPlainString());
                placeholders.put("nguongDoanhSo", milestone.getNguongDoanhSo().toPlainString());

                String to = emailService.resolveRecipient(card.getEmail());
                String body = emailService.render(template.getNoiDung(), placeholders);
                EmailService.EmailSendResult result = emailService.send(to, template.getTieuDe(), body);

                EmailLog log = new EmailLog();
                log.setCardId(card.getCardId());
                log.setLoaiThongBao(LOAI_DOANH_SO_MOC);
                log.setEmailTo(to);
                log.setNgayGui(LocalDateTime.now());
                log.setMilestoneId(milestone.getId());
                log.setCreatedAt(LocalDateTime.now());

                switch (result) {
                    case SUCCESS -> { log.setTrangThai("SUCCESS"); sent++; }
                    case FAILED -> { log.setTrangThai("FAILED"); log.setLoiChiTiet("Gửi email thất bại"); failed++; }
                    case SKIPPED_DISABLED -> { log.setTrangThai("SKIPPED_DISABLED"); skippedDisabled++; }
                }
                logsToInsert.add(log);
            }
        }

        emailLogBatchWriter.insertAll(logsToInsert);

        return JobRunResult.builder()
                .eligible(eligible)
                .sent(sent)
                .skippedDedup(skippedDedup)
                .skippedDisabled(skippedDisabled)
                .failed(failed)
                .build();
    }

    private static String nvl(String primary, String fallback) {
        return (primary != null && !primary.isBlank()) ? primary : (fallback != null ? fallback : "");
    }
}
