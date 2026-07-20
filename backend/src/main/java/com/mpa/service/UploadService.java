package com.mpa.service;

import com.mpa.dto.UploadBatchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import com.mpa.dto.UploadHistoryResponse;

import java.time.LocalDate;

public interface UploadService {

    UploadBatchResult handleUpload(MultipartFile[] files, LocalDate ngayDuLieu, String nguoiUpload);

    Page<UploadHistoryResponse> getHistory(Pageable pageable);
}
