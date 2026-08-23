package com.mpa.service;

import com.mpa.dto.ThongTinAmRequest;
import com.mpa.dto.ThongTinAmResponse;
import com.mpa.dto.ThongTinAmStatusCounts;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

public interface ThongTinAmService {
    Page<ThongTinAmResponse> getList(String search, Short trangThai, String maDonViCap6, int page, int size);
    List<ThongTinAmResponse> getAll(String search, Short trangThai, String maDonViCap6);
    /** Đếm tổng/hoạt động/không hoạt động — không lọc theo trangThai (chỉ theo search/phòng). */
    ThongTinAmStatusCounts getStatusCounts(String search, String maDonViCap6);
    ThongTinAmResponse create(ThongTinAmRequest request);
    ThongTinAmResponse update(Integer id, ThongTinAmRequest request);
    void delete(Integer id);
    List<Map<String, String>> getAmDropdown();
}
