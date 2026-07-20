package com.mpa.service.impl;

import com.mpa.dto.KhachHangChiTietResponse;
import com.mpa.dto.KpiSumResult;
import com.mpa.dto.ThongTinKhachHangRequest;
import com.mpa.dto.ThongTinKhachHangResponse;
import com.mpa.entity.PhongBan;
import com.mpa.entity.ThongTinAm;
import com.mpa.entity.ThongTinKhachHang;
import com.mpa.repository.PhongBanRepository;
import com.mpa.repository.ThongTinAmRepository;
import com.mpa.repository.ThongTinKhachHangRepository;
import com.mpa.repository.ThucHienBscKhachHangRepository;
import com.mpa.service.ThongTinKhachHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThongTinKhachHangServiceImpl implements ThongTinKhachHangService {

    private final ThongTinKhachHangRepository repo;
    private final ThongTinAmRepository amRepo;
    private final PhongBanRepository phongBanRepo;
    private final ThucHienBscKhachHangRepository bscKhRepo;

    private static final Map<Integer, String> PHAN_KHUC_MAP = Map.of(
        1, "Khối Bán Buôn",
        2, "Khối Bán Lẻ",
        3, "Khối FDI",
        4, "Khách hàng đặc biệt"
    );

    @Override
    public Page<ThongTinKhachHangResponse> getList(String search, Integer typeKhachHang, int page, int size) {
        Map<String, ThongTinAm>  amMap    = buildAmMap();
        Map<String, PhongBan>    phongMap = buildPhongMap();
        String s = search == null ? "" : search.trim();

        if (typeKhachHang == null) {
            return repo.searchAll(s, PageRequest.of(page, size))
                       .map(t -> toResponse(t, amMap, phongMap));
        }
        return repo.searchByType(s, typeKhachHang, PageRequest.of(page, size))
                   .map(t -> toResponse(t, amMap, phongMap));
    }

    @Override
    public ThongTinKhachHangResponse create(ThongTinKhachHangRequest req) {
        ThongTinKhachHang entity = new ThongTinKhachHang();
        applyRequest(entity, req);
        repo.save(entity);
        return toResponse(entity, buildAmMap(), buildPhongMap());
    }

    @Override
    public ThongTinKhachHangResponse update(Integer id, ThongTinKhachHangRequest req) {
        ThongTinKhachHang entity = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng id=" + id));
        applyRequest(entity, req);
        repo.save(entity);
        return toResponse(entity, buildAmMap(), buildPhongMap());
    }

    @Override
    public void delete(Integer id) {
        repo.deleteById(id);
    }

    @Override
    public List<Integer> getDistinctTypes() {
        return repo.findDistinctTypes();
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void applyRequest(ThongTinKhachHang entity, ThongTinKhachHangRequest req) {
        entity.setMaCn(req.getMaCn());
        entity.setMaDonViCap6(req.getMaDonViCap6());
        entity.setMaAm(req.getMaAm());
        entity.setMaKhCif(req.getMaKhCif());
        entity.setTenKhachHang(req.getTenKhachHang());
        entity.setSoDienThoai(req.getSoDienThoai());
        entity.setNgayBatDau(req.getNgayBatDau());
        entity.setEmail(req.getEmail());
        entity.setTypeKhachHang(req.getTypeKhachHang());
        entity.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : 1);
    }

    private ThongTinKhachHangResponse toResponse(ThongTinKhachHang t,
                                                   Map<String, ThongTinAm> amMap,
                                                   Map<String, PhongBan> phongMap) {
        ThongTinAm am    = (t.getMaAm() != null)         ? amMap.get(t.getMaAm())               : null;
        PhongBan   phong = (t.getMaDonViCap6() != null)  ? phongMap.get(t.getMaDonViCap6())      : null;

        return ThongTinKhachHangResponse.builder()
            .id(t.getId())
            .maCn(t.getMaCn())
            .maDonViCap6(t.getMaDonViCap6())
            .maAm(t.getMaAm())
            .tenAm(am != null ? am.getTenAm() : null)
            .tenPhong(phong != null ? phong.getTenDonViCap6() : null)
            .maKhCif(t.getMaKhCif())
            .tenKhachHang(t.getTenKhachHang())
            .soDienThoai(t.getSoDienThoai())
            .ngayBatDau(t.getNgayBatDau())
            .email(t.getEmail())
            .typeKhachHang(t.getTypeKhachHang())
            .tenPhanKhuc(mapPhanKhuc(t.getTypeKhachHang()))
            .trangThai(t.getTrangThai())
            .build();
    }

    private String mapPhanKhuc(Integer type) {
        if (type == null) return null;
        return PHAN_KHUC_MAP.getOrDefault(type, "Loại " + type);
    }

    private Map<String, ThongTinAm> buildAmMap() {
        return amRepo.findAll().stream()
            .filter(a -> a.getMaAm() != null)
            .collect(Collectors.toMap(ThongTinAm::getMaAm, a -> a, (x, y) -> x));
    }

    private Map<String, PhongBan> buildPhongMap() {
        return phongBanRepo.findAll().stream()
            .filter(p -> p.getMaDonViCap6() != null)
            .collect(Collectors.toMap(PhongBan::getMaDonViCap6, p -> p, (x, y) -> x));
    }

    @Override
    public KhachHangChiTietResponse getChiTiet(String cif, int nam, Integer thang, String quy, String soVoi) {
        ThongTinKhachHang kh = repo.findFirstByMaKhCifOrderByIdDesc(cif).orElse(null);

        String tenAm = null, tenPhong = null;
        if (kh != null) {
            Map<String, ThongTinAm> amMap = buildAmMap();
            Map<String, PhongBan>   phongMap = buildPhongMap();
            if (kh.getMaAm() != null) {
                ThongTinAm am = amMap.get(kh.getMaAm());
                if (am != null) tenAm = am.getTenAm();
            }
            if (kh.getMaDonViCap6() != null) {
                PhongBan phong = phongMap.get(kh.getMaDonViCap6());
                if (phong != null) tenPhong = phong.getTenDonViCap6();
            }
        }

        // ── Kỳ hiện tại ──────────────────────────────────────────────────
        KpiSumResult current;
        KpiSumResult prev;
        String kyHienTai;
        String kyTruoc;

        if (thang != null) {
            current = bscKhRepo.sumByCifAndThangNam(cif, thang, nam);
            kyHienTai = "Tháng " + String.format("%02d", thang) + "/" + nam;
            if ("cung-ky-nam-truoc".equals(soVoi)) {
                prev = bscKhRepo.sumByCifAndThangNam(cif, thang, nam - 1);
                kyTruoc = "Tháng " + String.format("%02d", thang) + "/" + (nam - 1);
            } else {
                int pt = thang == 1 ? 12 : thang - 1;
                int pn = thang == 1 ? nam - 1 : nam;
                prev = bscKhRepo.sumByCifAndThangNam(cif, pt, pn);
                kyTruoc = "Tháng " + String.format("%02d", pt) + "/" + pn;
            }
        } else if (quy != null) {
            current = bscKhRepo.sumByCifAndQuyNam(cif, quy, nam);
            kyHienTai = quy + "/" + nam;
            if ("cung-ky-nam-truoc".equals(soVoi)) {
                prev = bscKhRepo.sumByCifAndQuyNam(cif, quy, nam - 1);
                kyTruoc = quy + "/" + (nam - 1);
            } else {
                String pq = prevQuy(quy);
                int pn = "Q1".equals(quy) ? nam - 1 : nam;
                prev = bscKhRepo.sumByCifAndQuyNam(cif, pq, pn);
                kyTruoc = pq + "/" + pn;
            }
        } else {
            current = bscKhRepo.sumByCifAndNam(cif, nam);
            prev    = bscKhRepo.sumByCifAndNam(cif, nam - 1);
            kyHienTai = "Năm " + nam;
            kyTruoc   = "Năm " + (nam - 1);
        }

        // ── Benchmark (trung bình phân khúc – dùng subquery tránh IN list lớn) ──
        Integer type = kh != null ? kh.getTypeKhachHang() : null;

        KpiSumResult segTotal;
        BigDecimal bdCount;
        if (type == null) {
            segTotal = zeroKpi();
            bdCount  = BigDecimal.ONE;
        } else {
            List<Object[]> bRow;
            if (thang != null) {
                bRow = bscKhRepo.benchmarkByTypeThangNam(type, thang, nam);
            } else if (quy != null) {
                bRow = bscKhRepo.benchmarkByTypeQuyNam(type, quy, nam);
            } else {
                bRow = bscKhRepo.benchmarkByTypeNam(type, nam);
            }
            if (bRow == null || bRow.isEmpty()) {
                segTotal = zeroKpi();
                bdCount  = BigDecimal.ONE;
            } else {
                Object[] r = bRow.get(0);
                long cnt = r[0] instanceof Number n ? n.longValue() : 1L;
                bdCount  = BigDecimal.valueOf(cnt < 1 ? 1 : cnt);
                segTotal = new KpiSumResult(toBD(r[1]), toBD(r[2]), toBD(r[3]),
                                            toBD(r[4]), toBD(r[5]), toBD(r[6]), toBD(r[7]));
            }
        }

        // ── Trend (sparkline) ─────────────────────────────────────────────
        List<Object[]> trendRows = bscKhRepo.trendByThang(cif, nam);
        List<String>     trendLabels     = new ArrayList<>();
        List<BigDecimal> hdvTrend        = new ArrayList<>();
        List<BigDecimal> casaTrend       = new ArrayList<>();
        List<BigDecimal> duNoTrend       = new ArrayList<>();
        List<BigDecimal> tntDvTrend      = new ArrayList<>();
        List<BigDecimal> tntHdvTrend     = new ArrayList<>();
        List<BigDecimal> tntTdTrend      = new ArrayList<>();
        List<BigDecimal> tongTntTrend    = new ArrayList<>();

        for (Object[] row : trendRows) {
            trendLabels.add("T" + row[0]);
            hdvTrend.add(toBD(row[1]));
            casaTrend.add(toBD(row[2]));
            duNoTrend.add(toBD(row[3]));
            tntDvTrend.add(toBD(row[4]));
            tntHdvTrend.add(toBD(row[5]));
            tntTdTrend.add(toBD(row[6]));
            tongTntTrend.add(toBD(row[7]));
        }

        return KhachHangChiTietResponse.builder()
            .maKhCif(kh != null ? kh.getMaKhCif() : cif)
            .tenKhachHang(kh != null ? kh.getTenKhachHang() : "")
            .typeKhachHang(type)
            .tenPhanKhuc(mapPhanKhuc(type))
            .maAm(kh != null ? kh.getMaAm() : null)
            .tenAm(tenAm)
            .maDonViCap6(kh != null ? kh.getMaDonViCap6() : null)
            .tenDonViCap6(tenPhong)
            .soDienThoai(kh != null ? kh.getSoDienThoai() : null)
            .email(kh != null ? kh.getEmail() : null)
            .ngayBatDau(kh != null && kh.getNgayBatDau() != null ? kh.getNgayBatDau().toString() : null)
            // Kỳ hiện tại
            .hdvCuoiKy(current.getHuyDongVonCuoiKy())
            .casaBinhQuan(current.getCasaBinhQuan())
            .duNo(current.getDuNoTinDungCuoiKy())
            .tntDichVu(current.getThuNhapThuanDichVu())
            .tntHdv(current.getThuNhapThuanHdvFtp())
            .tntTinDung(current.getThuNhapThuanTinDung())
            .tongTnt(current.getThuNhapThuan())
            // Kỳ trước
            .hdvCuoiKyPrev(prev.getHuyDongVonCuoiKy())
            .casaBinhQuanPrev(prev.getCasaBinhQuan())
            .duNoPrev(prev.getDuNoTinDungCuoiKy())
            .tntDichVuPrev(prev.getThuNhapThuanDichVu())
            .tntHdvPrev(prev.getThuNhapThuanHdvFtp())
            .tntTinDungPrev(prev.getThuNhapThuanTinDung())
            .tongTntPrev(prev.getThuNhapThuan())
            // Benchmark
            .hdvCuoiKyTb(divSafe(segTotal.getHuyDongVonCuoiKy(), bdCount))
            .casaBinhQuanTb(divSafe(segTotal.getCasaBinhQuan(), bdCount))
            .duNoTb(divSafe(segTotal.getDuNoTinDungCuoiKy(), bdCount))
            .tntDichVuTb(divSafe(segTotal.getThuNhapThuanDichVu(), bdCount))
            .tntHdvTb(divSafe(segTotal.getThuNhapThuanHdvFtp(), bdCount))
            .tntTinDungTb(divSafe(segTotal.getThuNhapThuanTinDung(), bdCount))
            .tongTntTb(divSafe(segTotal.getThuNhapThuan(), bdCount))
            // Trend
            .trendLabels(trendLabels)
            .hdvCuoiKyTrend(hdvTrend)
            .casaBinhQuanTrend(casaTrend)
            .duNoTrend(duNoTrend)
            .tntDichVuTrend(tntDvTrend)
            .tntHdvTrend(tntHdvTrend)
            .tntTinDungTrend(tntTdTrend)
            .tongTntTrend(tongTntTrend)
            // Kỳ mô tả
            .kyHienTai(kyHienTai)
            .kyTruoc(kyTruoc)
            .build();
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private static String prevQuy(String quy) {
        return switch (quy) {
            case "Q2" -> "Q1";
            case "Q3" -> "Q2";
            case "Q4" -> "Q3";
            default   -> "Q4";
        };
    }

    private static BigDecimal toBD(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private static BigDecimal divSafe(BigDecimal a, BigDecimal b) {
        if (b == null || b.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return a.divide(b, 3, RoundingMode.HALF_UP);
    }

    private static KpiSumResult zeroKpi() {
        return new KpiSumResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
