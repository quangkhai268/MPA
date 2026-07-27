package com.mpa.service.impl;

import com.mpa.dto.DuLieuMpaSummary;
import com.mpa.entity.DuLieuMpa;
import com.mpa.repository.DuLieuMpaRepository;
import com.mpa.service.DuLieuMpaQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DuLieuMpaQueryServiceImpl implements DuLieuMpaQueryService {

    private final DuLieuMpaRepository repo;

    @Override
    public Page<DuLieuMpa> getList(String loaiKy, Integer thang, String quy, Integer nam, LocalDate ngay,
                                    String maDonViCap6, String search, int page, int size) {
        String loaiKyDb = normalizeLoaiKy(loaiKy);
        Integer thangEff = resolveThang(loaiKyDb, thang, nam);
        String searchTrim = normalizeSearch(search);
        return repo.search(loaiKyDb, thangEff, quy, nam, ngay,
                normalizePhong(maDonViCap6), searchTrim, PageRequest.of(page, size));
    }

    @Override
    public DuLieuMpaSummary getSummary(String loaiKy, Integer thang, String quy, Integer nam, LocalDate ngay,
                                        String maDonViCap6, String search) {
        String loaiKyDb = normalizeLoaiKy(loaiKy);
        Integer thangEff = resolveThang(loaiKyDb, thang, nam);
        String searchTrim = normalizeSearch(search);
        return repo.getSummary(loaiKyDb, thangEff, quy, nam, ngay, normalizePhong(maDonViCap6), searchTrim);
    }

    private String normalizeLoaiKy(String loaiKy) {
        return (loaiKy == null ? "thang" : loaiKy).toUpperCase();
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }

    private String normalizePhong(String maDonViCap6) {
        return (maDonViCap6 == null || maDonViCap6.isBlank()) ? null : maDonViCap6;
    }

    // loai_ky='NAM' là dữ liệu lũy kế theo nhiều mốc (cột thang = mốc lũy kế) — nếu
    // không truyền mốc cụ thể, phải tự chốt mốc mới nhất, nếu không sẽ lẫn nhiều mốc.
    private Integer resolveThang(String loaiKyDb, Integer thang, Integer nam) {
        if ("NAM".equals(loaiKyDb) && thang == null && nam != null) {
            return repo.findLatestThangForNamLuyKe(nam);
        }
        return thang;
    }

    @Override
    public List<Map<String, String>> getPhongList() {
        return repo.findDistinctPhongList().stream()
                .map(r -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("ma", (String) r[0]);
                    m.put("ten", r[1] != null ? (String) r[1] : (String) r[0]);
                    return m;
                })
                .collect(Collectors.toList());
    }
}
