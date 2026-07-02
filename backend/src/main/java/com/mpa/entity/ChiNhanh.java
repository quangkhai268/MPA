package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "chi_nhanh")
@Data
public class ChiNhanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_cn")
    private String maCn;

    @Column(name = "ten_cn")
    private String tenCn;

    @Column(name = "trangthai")
    private Short trangThai;
}
