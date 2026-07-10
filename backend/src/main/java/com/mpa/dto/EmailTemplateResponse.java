package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EmailTemplateResponse {
    private Integer id;
    private String loaiThongBao;
    private String tieuDe;
    private String noiDung;
    private Boolean active;
    private LocalDateTime updatedAt;
}
