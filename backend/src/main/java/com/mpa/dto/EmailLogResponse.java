package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EmailLogResponse {
    private Long id;
    private String cardId;
    private String loaiThongBao;
    private String emailTo;
    private LocalDateTime ngayGui;
    private String trangThai;
    private String loiChiTiet;
    private Integer campaignId;
    private Integer milestoneId;
}
