package com.mpa.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CampaignRequest {
    private String tenChienDich;
    private String moTa;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private String trangThai;
    private String tieuDeEmail;
    private String noiDungEmail;
    private List<CampaignCriteriaDto> criteria;
}
