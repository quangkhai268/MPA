package com.mpa.service.impl;

import com.mpa.dto.JobRunResult;
import com.mpa.entity.EmailLog;
import com.mpa.entity.EmailTemplate;
import com.mpa.entity.ThePhatHanh;
import com.mpa.repository.EmailLogRepository;
import com.mpa.repository.ThePhatHanhRepository;
import com.mpa.service.CardNotificationService;
import com.mpa.service.EmailService;
import com.mpa.service.EmailTemplateService;
import com.mpa.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class CardNotificationServiceImpl implements CardNotificationService {

    private static final String LOAI_CHUA_KICH_HOAT = "CHUA_KICH_HOAT";
    private static final String LOAI_CHUA_PSGD = "CHUA_PSGD";

    private final ThePhatHanhRepository thePhatHanhRepo;
    private final EmailLogRepository emailLogRepo;
    private final EmailLogBatchWriter emailLogBatchWriter;
    private final EmailTemplateService templateService;
    private final EmailService emailService;
    private final SystemSettingService settingService;

    @Override
    @Transactional
    public JobRunResult processChuaKichHoat() {
        int soNgayMin = settingService.getInt("CHUA_KICH_HOAT_SO_NGAY", 7);
        int lapLaiSoNgay = settingService.getInt("CHUA_KICH_HOAT_LAP_LAI_SO_NGAY", 7);
        List<ThePhatHanh> eligible = thePhatHanhRepo.findChuaKichHoatForNotify(soNgayMin);

        EmailTemplate template = templateService.getByLoaiThongBao(LOAI_CHUA_KICH_HOAT);

        return process(eligible, LOAI_CHUA_KICH_HOAT, template, lapLaiSoNgay, card -> {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("tenKhachHang", nvl(card.getTenChuTheChinh(), card.getHoTenKhachHangPht()));
            placeholders.put("soThe", nvl(card.getSoTheDaPhatHanh(), ""));
            placeholders.put("soNgay", String.valueOf(card.getSoNgayChuaKichHoat()));
            return placeholders;
        });
    }

    @Override
    @Transactional
    public JobRunResult processChuaPsgd() {
        int soNgay = settingService.getInt("CHUA_PSGD_SO_NGAY", 30);
        int lapLaiSoNgay = settingService.getInt("CHUA_PSGD_LAP_LAI_SO_NGAY", 15);
        LocalDateTime nguong = LocalDateTime.now().minusDays(soNgay);
        List<ThePhatHanh> eligible = thePhatHanhRepo.findChuaPsgdForNotify(nguong);

        EmailTemplate template = templateService.getByLoaiThongBao(LOAI_CHUA_PSGD);

        return process(eligible, LOAI_CHUA_PSGD, template, lapLaiSoNgay, card -> {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("tenKhachHang", nvl(card.getTenChuTheChinh(), card.getHoTenKhachHangPht()));
            placeholders.put("soThe", nvl(card.getSoTheDaPhatHanh(), ""));
            long soNgayThucTe = card.getNgayCapNhatTrangThaiCardContract() != null
                    ? java.time.Duration.between(card.getNgayCapNhatTrangThaiCardContract(), LocalDateTime.now()).toDays()
                    : soNgay;
            placeholders.put("soNgay", String.valueOf(soNgayThucTe));
            return placeholders;
        });
    }

    @Override
    @Transactional
    public EmailService.EmailSendResult testSendChuaKichHoatOne() {
        int soNgayMin = settingService.getInt("CHUA_KICH_HOAT_SO_NGAY", 7);
        List<ThePhatHanh> eligible = thePhatHanhRepo.findChuaKichHoatForNotify(soNgayMin);
        if (eligible.isEmpty()) {
            throw new RuntimeException("Không có thẻ nào đủ điều kiện 'chưa kích hoạt' để test gửi email");
        }
        ThePhatHanh card = eligible.get(0);
        EmailTemplate template = templateService.getByLoaiThongBao(LOAI_CHUA_KICH_HOAT);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tenKhachHang", nvl(card.getTenChuTheChinh(), card.getHoTenKhachHangPht()));
        placeholders.put("soThe", nvl(card.getSoTheDaPhatHanh(), ""));
        placeholders.put("soNgay", String.valueOf(card.getSoNgayChuaKichHoat()));

        String to = emailService.resolveRecipient(card.getEmail());
        String subject = template.getTieuDe();
        String body = emailService.render(template.getNoiDung(), placeholders);
        EmailService.EmailSendResult result = emailService.send(to, subject, body);

        EmailLog log = new EmailLog();
        log.setCardId(card.getCardId());
        log.setLoaiThongBao(LOAI_CHUA_KICH_HOAT);
        log.setEmailTo(to);
        log.setNgayGui(LocalDateTime.now());
        log.setCreatedAt(LocalDateTime.now());
        switch (result) {
            case SUCCESS -> log.setTrangThai("SUCCESS");
            case FAILED -> log.setTrangThai("FAILED");
            case SKIPPED_DISABLED -> log.setTrangThai("SKIPPED_DISABLED");
        }
        emailLogRepo.save(log);

        return result;
    }

    private JobRunResult process(List<ThePhatHanh> eligible, String loaiThongBao, EmailTemplate template,
                                  int lapLaiSoNgay,
                                  java.util.function.Function<ThePhatHanh, Map<String, String>> placeholderBuilder) {
        int sent = 0, skippedDedup = 0, skippedDisabled = 0, failed = 0;

        // 1 query cho cả job thay vì 1 query/thẻ — bắt buộc khi eligible có hàng chục nghìn thẻ.
        Set<String> daGuiTrongChuKy = emailLogRepo
                .findByLoaiThongBaoAndTrangThaiAndNgayGuiAfter(loaiThongBao, "SUCCESS", LocalDateTime.now().minusDays(lapLaiSoNgay))
                .stream().map(EmailLog::getCardId).collect(Collectors.toCollection(HashSet::new));

        List<EmailLog> logsToInsert = new ArrayList<>();

        for (ThePhatHanh card : eligible) {
            boolean dedup = daGuiTrongChuKy.contains(card.getCardId());
            if (dedup) {
                skippedDedup++;
                continue;
            }

            String to = emailService.resolveRecipient(card.getEmail());
            String subject = template.getTieuDe();
            String body = emailService.render(template.getNoiDung(), placeholderBuilder.apply(card));
            EmailService.EmailSendResult result = emailService.send(to, subject, body);

            EmailLog log = new EmailLog();
            log.setCardId(card.getCardId());
            log.setLoaiThongBao(loaiThongBao);
            log.setEmailTo(to);
            log.setNgayGui(LocalDateTime.now());
            log.setCreatedAt(LocalDateTime.now());

            switch (result) {
                case SUCCESS -> { log.setTrangThai("SUCCESS"); sent++; }
                case FAILED -> { log.setTrangThai("FAILED"); log.setLoiChiTiet("Gửi email thất bại"); failed++; }
                case SKIPPED_DISABLED -> { log.setTrangThai("SKIPPED_DISABLED"); skippedDisabled++; }
            }
            logsToInsert.add(log);
        }

        emailLogBatchWriter.insertAll(logsToInsert);

        return JobRunResult.builder()
                .eligible(eligible.size())
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
