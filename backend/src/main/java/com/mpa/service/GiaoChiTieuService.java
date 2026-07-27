package com.mpa.service;

import com.mpa.dto.BscSoSanhResponse;
import com.mpa.dto.ChiTieuBscRequest;
import com.mpa.dto.ChiTieuQuanLyRow;
import com.mpa.dto.XuHuongResponse;
import java.util.List;
import java.util.Map;

public interface GiaoChiTieuService {
    BscSoSanhResponse getSoSanh(String loaiKy, String selectedKy, String doiTuong, String maDonViCap6);

    void themChiTieu(ChiTieuBscRequest request);

    void deleteChiTieu(Integer id);

    List<ChiTieuQuanLyRow> getQuanLyList(String loaiKy, String selectedKy, String doiTuong, String maDonViCap6);

    List<Map<String, String>> getPhongList();

    List<Map<String, String>> getCnList();

    List<Map<String, String>> getAmList(String maDonViCap6);

    /** Ngày gần nhất có dữ liệu thực hiện (thuc_hien_bsc_chi_nhanh, loai_ky='NGAY'), định dạng dd/MM/yyyy. */
    String getLatestNgay();

    /** Dữ liệu 7 biểu đồ xu hướng (kế hoạch cột + thực hiện đường) cho tab "Xu hướng". */
    XuHuongResponse getXuHuong(String loaiKy, int nam, String doiTuong, String maDonViCap6, String maAm);
}
