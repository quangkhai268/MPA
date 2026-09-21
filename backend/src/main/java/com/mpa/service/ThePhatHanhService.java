package com.mpa.service;

import com.mpa.dto.KhachHangTheSummaryResponse;
import com.mpa.dto.ThePhatHanhDetailResponse;
import com.mpa.dto.ThePhatHanhResponse;
import com.mpa.dto.TheSummaryResponse;
import com.mpa.entity.ThePhatHanh;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ThePhatHanhService {

    Page<ThePhatHanhResponse> getList(
            String search, String trangThai, String hinhThuc, String productCode,
            String loaiTheTinDung, String maDonViCap6, String amSearch, List<String> amCodes,
            boolean chuaKichHoat, int soNgayMin, boolean chuaPsgd, boolean chuaDatPtn, boolean datPtn,
            int soNgayThuPtn, int page, int size);

    /** Toàn bộ thẻ khớp filter (không phân trang) — dùng cho Xuất Excel. */
    List<ThePhatHanh> exportList(
            String search, String trangThai, String hinhThuc, String productCode,
            String loaiTheTinDung, String maDonViCap6, String amSearch, List<String> amCodes,
            boolean chuaKichHoat, int soNgayMin, boolean chuaPsgd, boolean chuaDatPtn, boolean datPtn,
            int soNgayThuPtn);

    /** scopeMaDonViCap6 = null nghĩa là không giới hạn (ADMIN); khác null thì thẻ phải thuộc
     *  đúng phòng ban đó, nếu không sẽ coi như không tìm thấy — chặn user không phải ADMIN xem
     *  chi tiết thẻ phòng khác bằng cách gõ thẳng id trên URL. */
    ThePhatHanhDetailResponse getDetail(Long id, String scopeMaDonViCap6);

    /** scopeMaDonViCap6 = null nghĩa là không giới hạn (ADMIN), khác null thì mọi chỉ số chỉ
     *  tính trên thẻ thuộc phòng ban đó — khớp phạm vi với getList() ở trên. */
    TheSummaryResponse getSummary(String scopeMaDonViCap6);

    /** Tổng hợp thẻ tín dụng của 1 khách hàng — join thong_tin_khach_hang.ma_kh_cif = the_phat_hanh.so_cif_khach_hang_pht. */
    KhachHangTheSummaryResponse getSummaryByCif(String cif);

    List<String> getDistinctTrangThai();

    List<String> getDistinctHinhThuc();

    List<String> getDistinctProductCode();
}
