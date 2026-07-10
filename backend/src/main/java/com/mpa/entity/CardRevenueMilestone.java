package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_revenue_milestone")
@Data
public class CardRevenueMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "so_ngay_tu_phat_hanh")
    private Integer soNgayTuPhatHanh;

    @Column(name = "nguong_doanh_so")
    private BigDecimal nguongDoanhSo;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
