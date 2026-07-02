package com.mpa.service;

import com.mpa.dto.ThongTinAmRequest;
import com.mpa.dto.ThongTinAmResponse;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

public interface ThongTinAmService {
    Page<ThongTinAmResponse> getList(String search, Short trangThai, int page, int size);
    List<ThongTinAmResponse> getAll(String search, Short trangThai);
    ThongTinAmResponse create(ThongTinAmRequest request);
    ThongTinAmResponse update(Integer id, ThongTinAmRequest request);
    void delete(Integer id);
    List<Map<String, String>> getAmDropdown();
}
