package com.mpa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DuLieuMpaSummary {
    private Long soKhachHang;
    private BigDecimal tongTnt;
    private BigDecimal tongDuNo;
}
