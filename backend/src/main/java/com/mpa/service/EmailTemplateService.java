package com.mpa.service;

import com.mpa.dto.EmailTemplateRequest;
import com.mpa.dto.EmailTemplateResponse;
import com.mpa.entity.EmailTemplate;

import java.util.List;

public interface EmailTemplateService {

    List<EmailTemplateResponse> getAll();

    EmailTemplateResponse update(Integer id, EmailTemplateRequest request);

    /** Trả về entity gốc (không phải DTO) để CardNotificationService lấy noiDung render trực tiếp. */
    EmailTemplate getByLoaiThongBao(String loaiThongBao);
}
