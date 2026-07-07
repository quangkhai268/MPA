package com.mpa.service;

import com.mpa.dto.ThePhatHanhDetailResponse;
import com.mpa.dto.ThePhatHanhResponse;
import com.mpa.dto.TheSummaryResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ThePhatHanhService {

    Page<ThePhatHanhResponse> getList(
            String search, String trangThai, String hinhThuc, String productCode,
            String loaiTheTinDung,
            boolean chuaKichHoat, int soNgayMin, boolean chuaPsgd, boolean chuaDatPtn,
            int page, int size);

    ThePhatHanhDetailResponse getDetail(Long id);

    TheSummaryResponse getSummary();

    List<String> getDistinctTrangThai();

    List<String> getDistinctHinhThuc();

    List<String> getDistinctProductCode();
}
