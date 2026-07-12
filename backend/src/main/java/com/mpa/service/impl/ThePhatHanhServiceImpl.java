package com.mpa.service.impl;

import com.mpa.dto.ThePhatHanhDetailResponse;
import com.mpa.dto.ThePhatHanhResponse;
import com.mpa.dto.TheSummaryResponse;
import com.mpa.entity.ThePhatHanh;
import com.mpa.repository.ThePhatHanhRepository;
import com.mpa.service.ThePhatHanhService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThePhatHanhServiceImpl implements ThePhatHanhService {

    private final ThePhatHanhRepository repo;

    @Override
    public Page<ThePhatHanhResponse> getList(
            String search, String trangThai, String hinhThuc, String productCode,
            String loaiTheTinDung,
            boolean chuaKichHoat, int soNgayMin, boolean chuaPsgd, boolean chuaDatPtn,
            int page, int size) {

        String s   = (search == null) ? "" : search.trim();
        String tt  = (trangThai == null || trangThai.isBlank()) ? null : trangThai;
        String ht  = (hinhThuc == null || hinhThuc.isBlank()) ? null : hinhThuc;
        String pc  = (productCode == null || productCode.isBlank()) ? null : productCode;
        String ltd = (loaiTheTinDung == null || loaiTheTinDung.isBlank()) ? null : loaiTheTinDung;

        return repo.search(s, tt, ht, pc, ltd, chuaKichHoat, soNgayMin, chuaPsgd, chuaDatPtn, PageRequest.of(page, size))
                   .map(ThePhatHanhResponse::from);
    }

    @Override
    public ThePhatHanhDetailResponse getDetail(Long id) {
        ThePhatHanh entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thẻ id=" + id));
        return ThePhatHanhDetailResponse.from(entity);
    }

    @Override
    public TheSummaryResponse getSummary() {
        long total    = repo.count();
        long chuaKh   = repo.countChuaKichHoat();
        long chuaPsgd = repo.countChuaPsgd();
        long chuaPtn  = repo.countChuaDatPtn();
        long datPtn   = total - chuaPtn;

        BigDecimal hanMuc  = repo.sumHanMuc();
        BigDecimal doanhSo = repo.sumDoanhSo();
        long tongTdqt      = repo.countTdqt();
        long tdqtDatPtn    = repo.countTdqtDatPtn();

        long biKhoaCount = repo.findAll().stream()
                .filter(e -> {
                    String tt = e.getTrangThaiIssuingContract();
                    if (tt == null) return false;
                    String upper = tt.toUpperCase();
                    return upper.contains("KHÓA") || upper.contains("KHOA")
                            || upper.contains("BLOCK") || upper.contains("BLK");
                })
                .count();
        long hoatDong = Math.max(total - biKhoaCount - chuaKh, 0);

        // Tỷ lệ dùng hạn mức: dùng doanhSo / hanMuc làm proxy (view không có liab_top_contract)
        double tyLeDung = 0;
        if (hanMuc != null && hanMuc.compareTo(BigDecimal.ZERO) > 0
                && doanhSo != null && doanhSo.compareTo(BigDecimal.ZERO) > 0) {
            tyLeDung = doanhSo.divide(hanMuc, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        return TheSummaryResponse.builder()
                .tongSoThe(total)
                .soTheHoatDong(hoatDong)
                .soTheBiKhoa(biKhoaCount)
                .soTheChuaKichHoat(chuaKh)
                .soTheChuaPsgd(chuaPsgd)
                .hanMucCap(hanMuc != null ? hanMuc : BigDecimal.ZERO)
                .doanhSoGiaoDichMienPtn(doanhSo != null ? doanhSo : BigDecimal.ZERO)
                .tyLeDungHanMuc(tyLeDung)
                .soTheDatPtn(Math.max(datPtn, 0))
                .soTheChuaDatPtn(chuaPtn)
                .tongSoTdqt(tongTdqt)
                .soTdqtDatPtn(tdqtDatPtn)
                .build();
    }

    @Override
    public List<String> getDistinctTrangThai() {
        return repo.findDistinctTrangThai();
    }

    @Override
    public List<String> getDistinctHinhThuc() {
        return repo.findDistinctHinhThuc();
    }

    @Override
    public List<String> getDistinctProductCode() {
        return repo.findDistinctProductCode();
    }
}
