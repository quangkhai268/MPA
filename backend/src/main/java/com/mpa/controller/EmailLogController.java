package com.mpa.controller;

import com.mpa.dto.EmailLogResponse;
import com.mpa.entity.EmailLog;
import com.mpa.repository.EmailLogRepository;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-logs")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailLogRepository repo;

    @GetMapping
    public ApiResponse<Page<EmailLogResponse>> getList(
            @RequestParam(required = false) String cardId,
            @RequestParam(required = false) String loaiThongBao,
            @RequestParam(required = false) Integer campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<EmailLog> result = repo.search(cardId, loaiThongBao, campaignId,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayGui")));
            return ApiResponse.ok(result.map(this::toResponse));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải nhật ký email: " + e.getMessage());
        }
    }

    private EmailLogResponse toResponse(EmailLog e) {
        return EmailLogResponse.builder()
                .id(e.getId())
                .cardId(e.getCardId())
                .loaiThongBao(e.getLoaiThongBao())
                .emailTo(e.getEmailTo())
                .ngayGui(e.getNgayGui())
                .trangThai(e.getTrangThai())
                .loiChiTiet(e.getLoiChiTiet())
                .campaignId(e.getCampaignId())
                .milestoneId(e.getMilestoneId())
                .build();
    }
}
