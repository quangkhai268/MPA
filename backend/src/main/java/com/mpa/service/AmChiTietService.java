package com.mpa.service;

import com.mpa.dto.AmChiTietResponse;
import com.mpa.dto.XuHuongResponse;
import com.mpa.entity.ThongTinAm;

import java.util.List;

public interface AmChiTietService {

    /**
     * Chi tiết 1 mã AM (amEntities.size() == 1) hoặc gộp toàn bộ mã AM của 1 cán bộ
     * (amEntities.size() > 1) — kế hoạch/thực hiện/% hoàn thành/so kỳ trước cho kỳ đang chọn.
     */
    AmChiTietResponse getChiTiet(List<ThongTinAm> amEntities, boolean isCanBo, String loaiKy, String selectedKy);

    /** Xu hướng kế hoạch/thực hiện theo Tháng/Quý/Năm, gộp toàn bộ mã AM truyền vào. */
    XuHuongResponse getXuHuong(List<ThongTinAm> amEntities, String loaiKy, int nam);
}
