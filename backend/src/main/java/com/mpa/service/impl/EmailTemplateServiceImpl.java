package com.mpa.service.impl;

import com.mpa.dto.EmailTemplateRequest;
import com.mpa.dto.EmailTemplateResponse;
import com.mpa.entity.EmailTemplate;
import com.mpa.repository.EmailTemplateRepository;
import com.mpa.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private final EmailTemplateRepository repo;

    @Override
    public List<EmailTemplateResponse> getAll() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public EmailTemplateResponse update(Integer id, EmailTemplateRequest req) {
        EmailTemplate entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu email id=" + id));
        entity.setTieuDe(req.getTieuDe());
        entity.setNoiDung(req.getNoiDung());
        entity.setActive(req.getActive() != null ? req.getActive() : true);
        entity.setUpdatedAt(LocalDateTime.now());
        repo.save(entity);
        return toResponse(entity);
    }

    @Override
    public EmailTemplate getByLoaiThongBao(String loaiThongBao) {
        return repo.findByLoaiThongBao(loaiThongBao)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu email loại=" + loaiThongBao));
    }

    private EmailTemplateResponse toResponse(EmailTemplate e) {
        return EmailTemplateResponse.builder()
                .id(e.getId())
                .loaiThongBao(e.getLoaiThongBao())
                .tieuDe(e.getTieuDe())
                .noiDung(e.getNoiDung())
                .active(e.getActive())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
