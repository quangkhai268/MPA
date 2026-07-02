package com.mpa.service;

import com.mpa.dto.BscSoSanhResponse;
import com.mpa.dto.ChiTieuBscRequest;
import com.mpa.dto.ChiTieuQuanLyRow;
import java.util.List;
import java.util.Map;

public interface GiaoChiTieuService {
    BscSoSanhResponse getSoSanh(String loaiKy, String selectedKy, String doiTuong);

    void themChiTieu(ChiTieuBscRequest request);

    void deleteChiTieu(Integer id);

    List<ChiTieuQuanLyRow> getQuanLyList(String loaiKy, String selectedKy, String doiTuong);

    List<Map<String, String>> getPhongList();

    List<Map<String, String>> getCnList();

    List<Map<String, String>> getAmList();
}
