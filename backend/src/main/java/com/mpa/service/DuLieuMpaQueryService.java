package com.mpa.service;

import com.mpa.dto.DuLieuMpaSummary;
import com.mpa.entity.DuLieuMpa;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DuLieuMpaQueryService {
    Page<DuLieuMpa> getList(String loaiKy, Integer thang, String quy, Integer nam, LocalDate ngay,
                             String maDonViCap6, String search, int page, int size);

    DuLieuMpaSummary getSummary(String loaiKy, Integer thang, String quy, Integer nam, LocalDate ngay,
                                 String maDonViCap6, String search);

    List<Map<String, String>> getPhongList();
}
