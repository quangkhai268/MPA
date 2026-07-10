package com.mpa.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CampaignPreviewResponse {
    private long soLuong;
    private List<ThePhatHanhResponse> mauKhachHang;
}
