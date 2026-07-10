package com.mpa.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CardRevenueMilestoneRequest {
    private Integer soNgayTuPhatHanh;
    private BigDecimal nguongDoanhSo;
    private String moTa;
    private Boolean active;
}
