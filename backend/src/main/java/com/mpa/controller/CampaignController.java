package com.mpa.controller;

import com.mpa.dto.*;
import com.mpa.entity.EmailLog;
import com.mpa.repository.EmailLogRepository;
import com.mpa.service.CampaignService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService service;
    private final EmailLogRepository emailLogRepo;

    @GetMapping
    public ApiResponse<List<CampaignResponse>> getAll() {
        try {
            return ApiResponse.ok(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách chiến dịch: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignResponse> getById(@PathVariable Integer id) {
        try {
            return ApiResponse.ok(service.getById(id));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải chiến dịch: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<CampaignResponse> create(@RequestBody CampaignRequest request) {
        try {
            return ApiResponse.ok(service.create(request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tạo chiến dịch: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<CampaignResponse> update(@PathVariable Integer id, @RequestBody CampaignRequest request) {
        try {
            return ApiResponse.ok(service.update(id, request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật chiến dịch: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        try {
            service.delete(id);
            return ApiResponse.ok(null);
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi xóa chiến dịch: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/preview")
    public ApiResponse<CampaignPreviewResponse> preview(@PathVariable Integer id) {
        try {
            return ApiResponse.ok(service.preview(id));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi xem trước chiến dịch: " + e.getMessage());
        }
    }

    @PostMapping("/preview-by-criteria")
    public ApiResponse<CampaignPreviewResponse> previewByCriteria(@RequestBody CampaignRequest request) {
        try {
            return ApiResponse.ok(service.previewByCriteria(request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi xem trước tiêu chí: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/send")
    public ApiResponse<JobRunResult> send(@PathVariable Integer id, @RequestParam(defaultValue = "true") boolean testMode) {
        try {
            return ApiResponse.ok(service.send(id, testMode));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi gửi chiến dịch: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<Page<EmailLogResponse>> getLogs(@PathVariable Integer id,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        try {
            Page<EmailLog> logs = emailLogRepo.search(null, null, id,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayGui")));
            return ApiResponse.ok(logs.map(this::toLogResponse));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải nhật ký chiến dịch: " + e.getMessage());
        }
    }

    private EmailLogResponse toLogResponse(EmailLog e) {
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

    @GetMapping("/criteria-options")
    public ApiResponse<Map<String, List<String>>> getCriteriaOptions() {
        try {
            return ApiResponse.ok(service.getCriteriaOptions());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải tùy chọn tiêu chí: " + e.getMessage());
        }
    }
}
