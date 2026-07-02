package com.mpa.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ChiTieuBscRequest {
    private String loaiKy;
    private String selectedKy;
    private String doiTuong;   // "chi-nhanh", "phong", "am"
    private String maUnit;
    private String tenUnit;
    private String chiTieu;    // "hdv-cuoi-ky", "casa-bq", "du-no", "tnt-dv", "tnt-hdv", "tnt-td", "tong-tnt"
    private BigDecimal mucTieu; // in triệu VND
}
