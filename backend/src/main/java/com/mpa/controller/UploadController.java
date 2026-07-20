package com.mpa.controller;

import com.mpa.dto.UploadBatchResult;
import com.mpa.dto.UploadHistoryResponse;
import com.mpa.service.UploadService;
import com.mpa.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService service;

    @PostMapping
    public ApiResponse<UploadBatchResult> upload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("ngayDuLieu") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayDuLieu,
            @RequestParam(required = false) String nguoiUpload) {
        try {
            if (files == null || files.length == 0) {
                return ApiResponse.error("Vui lòng chọn ít nhất 1 file");
            }
            return ApiResponse.ok(service.handleUpload(files, ngayDuLieu, nguoiUpload));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải lên: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ApiResponse<Page<UploadHistoryResponse>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ApiResponse.ok(service.getHistory(PageRequest.of(page, size)));
        } catch (Exception e) {
            return ApiResponse.error("Lỗi khi tải lịch sử upload: " + e.getMessage());
        }
    }
}
