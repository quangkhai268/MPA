package com.mpa.service.impl;

import com.mpa.dto.*;
import com.mpa.entity.Campaign;
import com.mpa.entity.CampaignCriteria;
import com.mpa.entity.EmailLog;
import com.mpa.entity.ThePhatHanh;
import com.mpa.repository.CampaignCriteriaRepository;
import com.mpa.repository.CampaignRepository;
import com.mpa.repository.EmailLogRepository;
import com.mpa.repository.ThePhatHanhRepository;
import com.mpa.service.CampaignService;
import com.mpa.service.EmailService;
import com.mpa.util.CampaignCriteriaSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
public class CampaignServiceImpl implements CampaignService {

    private static final String LOAI_CAMPAIGN = "CAMPAIGN";

    private final CampaignRepository campaignRepo;
    private final CampaignCriteriaRepository criteriaRepo;
    private final ThePhatHanhRepository thePhatHanhRepo;
    private final EmailLogRepository emailLogRepo;
    private final EmailLogBatchWriter emailLogBatchWriter;
    private final EmailService emailService;

    @Override
    public List<CampaignResponse> getAll() {
        return campaignRepo.findAll().stream().map(c -> toResponse(c, criteriaRepo.findByCampaignId(c.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public CampaignResponse getById(Integer id) {
        Campaign c = findCampaign(id);
        return toResponse(c, criteriaRepo.findByCampaignId(id));
    }

    @Override
    @Transactional
    public CampaignResponse create(CampaignRequest req) {
        Campaign entity = new Campaign();
        applyRequest(entity, req);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy("admin");
        campaignRepo.save(entity);
        saveCriteria(entity.getId(), req.getCriteria());
        return toResponse(entity, criteriaRepo.findByCampaignId(entity.getId()));
    }

    @Override
    @Transactional
    public CampaignResponse update(Integer id, CampaignRequest req) {
        Campaign entity = findCampaign(id);
        applyRequest(entity, req);
        entity.setUpdatedAt(LocalDateTime.now());
        campaignRepo.save(entity);
        criteriaRepo.deleteByCampaignId(id);
        saveCriteria(id, req.getCriteria());
        return toResponse(entity, criteriaRepo.findByCampaignId(id));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        criteriaRepo.deleteByCampaignId(id);
        campaignRepo.deleteById(id);
    }

    @Override
    public CampaignPreviewResponse preview(Integer id) {
        findCampaign(id); // đảm bảo tồn tại
        List<CampaignCriteria> criteria = criteriaRepo.findByCampaignId(id);
        return buildPreview(criteria);
    }

    @Override
    public CampaignPreviewResponse previewByCriteria(CampaignRequest req) {
        List<CampaignCriteria> criteria = toEntityCriteria(null, req.getCriteria());
        return buildPreview(criteria);
    }

    @Override
    @Transactional
    public JobRunResult send(Integer id) {
        Campaign campaign = findCampaign(id);
        List<CampaignCriteria> criteria = criteriaRepo.findByCampaignId(id);
        Specification<ThePhatHanh> spec = CampaignCriteriaSpecificationBuilder.build(criteria);
        List<ThePhatHanh> matches = thePhatHanhRepo.findAll(spec);

        int eligible = 0, sent = 0, skippedDedup = 0, skippedDisabled = 0, failed = 0;

        // 1 query cho cả chiến dịch thay vì 1 query/thẻ — chiến dịch có thể khớp hàng chục nghìn thẻ.
        Set<String> daGuiTrongChienDich = emailLogRepo.findByCampaignIdAndTrangThai(id, "SUCCESS")
                .stream().map(EmailLog::getCardId).collect(Collectors.toCollection(HashSet::new));

        List<EmailLog> logsToInsert = new ArrayList<>();

        for (ThePhatHanh card : matches) {
            if (card.getEmail() == null || card.getEmail().isBlank()) continue;
            eligible++;

            boolean daGuiRoi = daGuiTrongChienDich.contains(card.getCardId());
            if (daGuiRoi) {
                skippedDedup++;
                continue;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("tenKhachHang", nvl(card.getTenChuTheChinh(), card.getHoTenKhachHangPht()));
            placeholders.put("soThe", nvl(card.getSoTheDaPhatHanh(), ""));

            String to = emailService.resolveRecipient(card.getEmail());
            String body = emailService.render(campaign.getNoiDungEmail(), placeholders);
            EmailService.EmailSendResult result = emailService.send(to, campaign.getTieuDeEmail(), body);

            EmailLog log = new EmailLog();
            log.setCardId(card.getCardId());
            log.setLoaiThongBao(LOAI_CAMPAIGN);
            log.setEmailTo(to);
            log.setNgayGui(LocalDateTime.now());
            log.setCampaignId(id);
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
                .eligible(eligible)
                .sent(sent)
                .skippedDedup(skippedDedup)
                .skippedDisabled(skippedDisabled)
                .failed(failed)
                .build();
    }

    @Override
    public Map<String, List<String>> getCriteriaOptions() {
        Map<String, List<String>> options = new HashMap<>();
        options.put("loaiThe", thePhatHanhRepo.findDistinctLoaiThe());
        options.put("productCode", thePhatHanhRepo.findDistinctProductCode());
        options.put("hinhThucThe", thePhatHanhRepo.findDistinctHinhThuc());
        options.put("nhomKhThe", thePhatHanhRepo.findDistinctNhomKhThe());
        options.put("trangThaiThe", thePhatHanhRepo.findDistinctTrangThai());
        return options;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private CampaignPreviewResponse buildPreview(List<CampaignCriteria> criteria) {
        Specification<ThePhatHanh> spec = CampaignCriteriaSpecificationBuilder.build(criteria);
        long count = thePhatHanhRepo.count(spec);
        List<ThePhatHanhResponse> sample = thePhatHanhRepo.findAll(spec, PageRequest.of(0, 20))
                .map(ThePhatHanhResponse::from)
                .getContent();
        return CampaignPreviewResponse.builder().soLuong(count).mauKhachHang(sample).build();
    }

    private Campaign findCampaign(Integer id) {
        return campaignRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch id=" + id));
    }

    private void applyRequest(Campaign entity, CampaignRequest req) {
        entity.setTenChienDich(req.getTenChienDich());
        entity.setMoTa(req.getMoTa());
        entity.setNgayBatDau(req.getNgayBatDau());
        entity.setNgayKetThuc(req.getNgayKetThuc());
        entity.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : "DRAFT");
        entity.setTieuDeEmail(req.getTieuDeEmail());
        entity.setNoiDungEmail(req.getNoiDungEmail());
    }

    private void saveCriteria(Integer campaignId, List<CampaignCriteriaDto> criteria) {
        if (criteria == null) return;
        for (CampaignCriteriaDto dto : criteria) {
            CampaignCriteria entity = new CampaignCriteria();
            entity.setCampaignId(campaignId);
            entity.setTieuChiField(dto.getTieuChiField());
            entity.setTieuChiValue(dto.getTieuChiValue());
            criteriaRepo.save(entity);
        }
    }

    private List<CampaignCriteria> toEntityCriteria(Integer campaignId, List<CampaignCriteriaDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(dto -> {
            CampaignCriteria entity = new CampaignCriteria();
            entity.setCampaignId(campaignId);
            entity.setTieuChiField(dto.getTieuChiField());
            entity.setTieuChiValue(dto.getTieuChiValue());
            return entity;
        }).collect(Collectors.toList());
    }

    private CampaignResponse toResponse(Campaign c, List<CampaignCriteria> criteria) {
        List<CampaignCriteriaDto> criteriaDtos = criteria.stream().map(cr -> {
            CampaignCriteriaDto dto = new CampaignCriteriaDto();
            dto.setTieuChiField(cr.getTieuChiField());
            dto.setTieuChiValue(cr.getTieuChiValue());
            return dto;
        }).collect(Collectors.toList());

        return CampaignResponse.builder()
                .id(c.getId())
                .tenChienDich(c.getTenChienDich())
                .moTa(c.getMoTa())
                .ngayBatDau(c.getNgayBatDau())
                .ngayKetThuc(c.getNgayKetThuc())
                .trangThai(c.getTrangThai())
                .tieuDeEmail(c.getTieuDeEmail())
                .noiDungEmail(c.getNoiDungEmail())
                .criteria(criteriaDtos)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private static String nvl(String primary, String fallback) {
        return (primary != null && !primary.isBlank()) ? primary : (fallback != null ? fallback : "");
    }
}
