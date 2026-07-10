package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CampaignResponse {
    private Integer id;
    private String tenChienDich;
    private String moTa;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private String trangThai;
    private String tieuDeEmail;
    private String noiDungEmail;
    private List<CampaignCriteriaDto> criteria;
    private LocalDateTime createdAt;
}
