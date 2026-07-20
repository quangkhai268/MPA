package com.mpa.dto;

import com.mpa.entity.UploadHistory;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UploadHistoryResponse {
    private Long id;
    private LocalDateTime thoiGian;
    private String nguoiUpload;
    private LocalDate ngayDuLieu;
    private Integer soFile;
    private Integer tongDong;
    private String trangThai;

    public static UploadHistoryResponse from(UploadHistory e) {
        UploadHistoryResponse r = new UploadHistoryResponse();
        r.id = e.getId();
        r.thoiGian = e.getThoiGian();
        r.nguoiUpload = e.getNguoiUpload();
        r.ngayDuLieu = e.getNgayDuLieu();
        r.soFile = e.getSoFile();
        r.tongDong = e.getTongDong();
        r.trangThai = e.getTrangThai();
        return r;
    }
}
