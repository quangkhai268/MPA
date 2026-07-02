package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "phong_ban")
@Data
public class PhongBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_don_vi_cap_6", unique = true)
    private String maDonViCap6;

    @Column(name = "ten_don_vi_cap_6")
    private String tenDonViCap6;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ma_cn")
    private String maCn;

    @Column(name = "ten_cn")
    private String tenCn;

    @Column(name = "trangthai")
    private Short trangThai;
}
