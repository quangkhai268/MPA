package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CardRevenueMilestoneResponse {
    private Integer id;
    private Integer soNgayTuPhatHanh;
    private BigDecimal nguongDoanhSo;
    private String moTa;
    private Boolean active;
}
