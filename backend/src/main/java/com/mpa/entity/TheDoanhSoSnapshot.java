package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "the_doanh_so_snapshot")
@Data
public class TheDoanhSoSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id")
    private String cardId;

    @Column(name = "ngay_snapshot")
    private LocalDate ngaySnapshot;

    @Column(name = "doanh_so_luy_ke")
    private BigDecimal doanhSoLuyKe;

    @Column(name = "so_ngay_tu_phat_hanh")
    private Integer soNgayTuPhatHanh;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
