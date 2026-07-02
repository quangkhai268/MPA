package com.mpa.service;

import com.mpa.dto.KhachHangChiTietResponse;
import com.mpa.dto.ThongTinKhachHangRequest;
import com.mpa.dto.ThongTinKhachHangResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ThongTinKhachHangService {
    Page<ThongTinKhachHangResponse> getList(String search, Integer typeKhachHang, int page, int size);
    ThongTinKhachHangResponse create(ThongTinKhachHangRequest request);
    ThongTinKhachHangResponse update(Integer id, ThongTinKhachHangRequest request);
    void delete(Integer id);
    List<Integer> getDistinctTypes();
    KhachHangChiTietResponse getChiTiet(String cif, int nam, Integer thang, String quy, String soVoi);
}
