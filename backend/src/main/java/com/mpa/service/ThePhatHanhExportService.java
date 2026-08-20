package com.mpa.service;

import com.mpa.entity.ThePhatHanh;

import java.util.List;

public interface ThePhatHanhExportService {

    /** Ghi danh sách thẻ ra file Excel (.xlsx), trả về nội dung file dạng byte[]. */
    byte[] exportExcel(List<ThePhatHanh> cards);
}
