package com.mpa.controller;

import com.mpa.dto.CardRevenueMilestoneRequest;
import com.mpa.dto.CardRevenueMilestoneResponse;
import com.mpa.dto.JobRunResult;
import com.mpa.service.CardMilestoneEvaluationService;
import com.mpa.service.CardRevenueMilestoneService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/card-revenue-milestones")
@RequiredArgsConstructor
public class CardRevenueMilestoneController {

    private final CardRevenueMilestoneService service;
    private final CardMilestoneEvaluationService evaluationService;

    @GetMapping
    public ApiResponse<List<CardRevenueMilestoneResponse>> getAll() {
        try {
            return ApiResponse.ok(service.getAll());
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải danh sách mốc doanh số: " + e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<CardRevenueMilestoneResponse> create(@RequestBody CardRevenueMilestoneRequest request) {
        try {
            return ApiResponse.ok(service.create(request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi thêm mốc doanh số: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<CardRevenueMilestoneResponse> update(@PathVariable Integer id,
                                                             @RequestBody CardRevenueMilestoneRequest request) {
        try {
            return ApiResponse.ok(service.update(id, request));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi cập nhật mốc doanh số: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        try {
            service.delete(id);
            return ApiResponse.ok(null);
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi xóa mốc doanh số: " + e.getMessage());
        }
    }

    @PostMapping("/run")
    public ApiResponse<JobRunResult> run(@RequestParam(defaultValue = "true") boolean testMode) {
        try {
            return ApiResponse.ok(evaluationService.evaluateAndNotify(testMode));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi chạy đánh giá mốc doanh số: " + e.getMessage());
        }
    }
}
