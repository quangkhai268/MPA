package com.mpa.service.impl;

import com.mpa.dto.KhachHangTheSummaryResponse;
import com.mpa.dto.ThePhatHanhDetailResponse;
import com.mpa.dto.ThePhatHanhResponse;
import com.mpa.dto.TheSummaryResponse;
import com.mpa.entity.ThePhatHanh;
import com.mpa.repository.ThePhatHanhRepository;
import com.mpa.service.ThePhatHanhService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThePhatHanhServiceImpl implements ThePhatHanhService {

    private final ThePhatHanhRepository repo;

    @Override
    public Page<ThePhatHanhResponse> getList(
            String search, String trangThai, String hinhThuc, String productCode,
            String loaiTheTinDung, String maDonViCap6, String amSearch, List<String> amCodes,
            boolean chuaKichHoat, int soNgayMin, boolean chuaPsgd, boolean chuaDatPtn, boolean datPtn,
            int soNgayThuPtn, int page, int size) {

        FilterParams f = normalize(search, trangThai, hinhThuc, productCode, loaiTheTinDung, maDonViCap6, amSearch, amCodes);
        LocalDate hanThuPtn = LocalDate.now().plusDays(Math.max(soNgayThuPtn, 0));

        return repo.search(f.search, f.trangThai, f.hinhThuc, f.productCode, f.loaiTheTinDung, f.maDonViCap6,
                        f.amSearch, f.amCodes, chuaKichHoat, soNgayMin, chuaPsgd, chuaDatPtn, datPtn,
                        soNgayThuPtn, hanThuPtn, PageRequest.of(page, size))
                   .map(ThePhatHanhResponse::from);
    }

    @Override
    public List<ThePhatHanh> exportList(
            String search, String trangThai, String hinhThuc, String productCode,
            String loaiTheTinDung, String maDonViCap6, String amSearch, List<String> amCodes,
            boolean chuaKichHoat, int soNgayMin, boolean chuaPsgd, boolean chuaDatPtn, boolean datPtn,
            int soNgayThuPtn) {

        FilterParams f = normalize(search, trangThai, hinhThuc, productCode, loaiTheTinDung, maDonViCap6, amSearch, amCodes);
        LocalDate hanThuPtn = LocalDate.now().plusDays(Math.max(soNgayThuPtn, 0));

        return repo.search(f.search, f.trangThai, f.hinhThuc, f.productCode, f.loaiTheTinDung, f.maDonViCap6,
                        f.amSearch, f.amCodes, chuaKichHoat, soNgayMin, chuaPsgd, chuaDatPtn, datPtn,
                        soNgayThuPtn, hanThuPtn, org.springframework.data.domain.Pageable.unpaged())
                   .getContent();
    }

    private record FilterParams(String search, String trangThai, String hinhThuc, String productCode,
                                 String loaiTheTinDung, String maDonViCap6, String amSearch, List<String> amCodes) {}

    private FilterParams normalize(String search, String trangThai, String hinhThuc, String productCode,
                                    String loaiTheTinDung, String maDonViCap6, String amSearch, List<String> amCodes) {
        return new FilterParams(
                (search == null) ? "" : search.trim(),
                (trangThai == null || trangThai.isBlank()) ? null : trangThai,
                (hinhThuc == null || hinhThuc.isBlank()) ? null : hinhThuc,
                (productCode == null || productCode.isBlank()) ? null : productCode,
                (loaiTheTinDung == null || loaiTheTinDung.isBlank()) ? null : loaiTheTinDung,
                (maDonViCap6 == null || maDonViCap6.isBlank()) ? null : maDonViCap6,
                (amSearch == null) ? "" : amSearch.trim(),
                (amCodes == null || amCodes.isEmpty()) ? null : amCodes);
    }

    @Override
    public ThePhatHanhDetailResponse getDetail(Long id) {
        ThePhatHanh entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thẻ id=" + id));
        return ThePhatHanhDetailResponse.from(entity);
    }

    @Override
    @Cacheable("thePhatHanhSummary")
    public TheSummaryResponse getSummary() {
        long total    = repo.count();
        long chuaKh   = repo.countChuaKichHoat();
        long chuaPsgd = repo.countChuaPsgd();
        // countChuaDatPtn/countTdqt đã giới hạn theo đúng phạm vi thẻ đủ điều kiện xét PTN
        // (TDQT, không thuộc 4 trạng thái Auto-Closed/Closed/Fraud/Lost) — datPtn tính trên
        // cùng phạm vi đó, không phải trừ trên tổng toàn bộ thẻ.
        long chuaPtn  = repo.countChuaDatPtn();

        BigDecimal hanMuc  = repo.sumHanMuc();
        BigDecimal doanhSo = repo.sumDoanhSo();
        long tongTdqt      = repo.countTdqt();
        long tdqtDatPtn    = repo.countTdqtDatPtn();
        long datPtn        = Math.max(tongTdqt - chuaPtn, 0);

        long biKhoaCount = repo.countBiKhoa();
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
    public KhachHangTheSummaryResponse getSummaryByCif(String cif) {
        List<ThePhatHanh> cards = repo.findBySoCifKhachHangPhtOrderByIdDesc(cif);

        int soHopDong = cards.size();
        int soChuaActive = (int) cards.stream()
                .filter(c -> c.getSoNgayChuaKichHoat() != null && c.getSoNgayChuaKichHoat() > 0)
                .count();
        int soKhoa = (int) cards.stream()
                .filter(c -> isKhoa(c.getTrangThaiIssuingContract()))
                .count();
        // Phí thường niên chỉ tính với thẻ Tín dụng QT và không thuộc 4 trạng thái
        // Auto-Closed/Closed/Fraud/Lost — cùng nguyên tắc áp dụng ở ThePhatHanhRepository.search().
        // Thẻ có so_tien_phi_thuong_nien = 0 (miễn phí thường niên luôn) cũng tính là đã đạt.
        int soChuaDatPtn = (int) cards.stream()
                .filter(c -> "TDQT".equals(c.getLoaiTheTinDung()) && !isDongThe(c.getTrangThaiThe()))
                .filter(c -> {
                    BigDecimal phi = c.getSoTienPhiThuongNien();
                    if (phi != null && phi.compareTo(BigDecimal.ZERO) == 0) return false;
                    BigDecimal ds = c.getDoanhSoGiaoDichMienPtn();
                    BigDecimal muc = c.getDoanhSoMienPtn();
                    return ds == null || muc == null || ds.compareTo(muc) < 0;
                })
                .count();

        BigDecimal tongHanMuc = cards.stream()
                .map(c -> c.getHmtdIssuingContract() != null ? c.getHmtdIssuingContract() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tongDoanhSo = cards.stream()
                .map(c -> c.getDoanhSoGiaoDichMienPtn() != null ? c.getDoanhSoGiaoDichMienPtn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double tyLe = 0;
        if (tongHanMuc.compareTo(BigDecimal.ZERO) > 0) {
            tyLe = tongDoanhSo.divide(tongHanMuc, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        List<ThePhatHanhResponse> theList = cards.stream()
                .map(ThePhatHanhResponse::from)
                .collect(Collectors.toList());

        return KhachHangTheSummaryResponse.builder()
                .soHopDong(soHopDong)
                .soKhoa(soKhoa)
                .soChuaActive(soChuaActive)
                .tongHanMuc(tongHanMuc)
                .tongDoanhSo(tongDoanhSo)
                .tyLeDoanhSoTrenHanMuc(tyLe)
                .soTheChuaDatPtn(soChuaDatPtn)
                .theList(theList)
                .build();
    }

    private boolean isKhoa(String trangThaiIssuingContract) {
        if (trangThaiIssuingContract == null) return false;
        String upper = trangThaiIssuingContract.toUpperCase();
        return upper.contains("KHÓA") || upper.contains("KHOA")
                || upper.contains("BLOCK") || upper.contains("BLK");
    }

    /** Auto-Closed/Closed/Fraud/Lost — thẻ ở 1 trong 4 trạng thái này không tính vào PTN. */
    private static final java.util.Set<String> TRANG_THAI_KHONG_TINH_PTN = java.util.Set.of(
            "Card Auto-Closed", "Card Closed", "Card Fraud", "Card Lost");

    private boolean isDongThe(String trangThaiThe) {
        return trangThaiThe != null && TRANG_THAI_KHONG_TINH_PTN.contains(trangThaiThe);
    }

    @Override
    @Cacheable("thePhatHanhTrangThai")
    public List<String> getDistinctTrangThai() {
        return repo.findDistinctTrangThai();
    }

    @Override
    @Cacheable("thePhatHanhHinhThuc")
    public List<String> getDistinctHinhThuc() {
        return repo.findDistinctHinhThuc();
    }

    @Override
    @Cacheable("thePhatHanhProductCode")
    public List<String> getDistinctProductCode() {
        return repo.findDistinctProductCode();
    }
}
