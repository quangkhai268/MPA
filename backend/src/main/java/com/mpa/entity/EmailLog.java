package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_log")
@Data
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id")
    private String cardId;

    @Column(name = "loai_thong_bao")
    private String loaiThongBao;

    @Column(name = "email_to")
    private String emailTo;

    @Column(name = "ngay_gui")
    private LocalDateTime ngayGui;

    @Column(name = "trang_thai")
    private String trangThai;

    @Column(name = "loi_chi_tiet")
    private String loiChiTiet;

    @Column(name = "campaign_id")
    private Integer campaignId;

    @Column(name = "milestone_id")
    private Integer milestoneId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
