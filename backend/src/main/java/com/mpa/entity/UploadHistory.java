package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "upload_history")
@Data
public class UploadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;

    @Column(name = "nguoi_upload")
    private String nguoiUpload;

    @Column(name = "ngay_du_lieu")
    private LocalDate ngayDuLieu;

    @Column(name = "so_file")
    private Integer soFile;

    @Column(name = "tong_dong")
    private Integer tongDong;

    @Column(name = "trang_thai")
    private String trangThai;

    @Column(name = "chi_tiet")
    private String chiTiet;
}
