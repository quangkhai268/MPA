package com.mpa.service.impl;

import com.mpa.dto.AmChiTietResponse;
import com.mpa.dto.XuHuongMetricSeries;
import com.mpa.dto.XuHuongResponse;
import com.mpa.entity.PhongBan;
import com.mpa.entity.ThongTinAm;
import com.mpa.repository.ChiTieuBscRepository;
import com.mpa.repository.PhongBanRepository;
import com.mpa.repository.ThePhatHanhRepository;
import com.mpa.repository.ThucHienBscRepository;
import com.mpa.service.AmChiTietService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Chi tiết theo mã AM / theo cán bộ (gộp nhiều mã AM) — dùng riêng cho màn hình
 * quan-ly-am/:maAm và quan-ly-am/can-bo/:tenAm. Đọc dữ liệu qua đúng các nguồn đã
 * dùng ở GiaoChiTieuServiceImpl (kế hoạch: ChiTieuBscRepository.keHoachAmBy... /
 * keHoachTrendAmBy..., thực hiện: ThucHienBscRepository.amRowBy... / trendAmBy...
 * — luôn type_data=3) nhưng KHÔNG sửa GiaoChiTieuServiceImpl để tránh ảnh hưởng
 * màn hình Giao chỉ tiêu đang chạy ổn định.
 */
@Service
@RequiredArgsConstructor
public class AmChiTietServiceImpl implements AmChiTietService {

    private final ChiTieuBscRepository chiTieuRepo;
    private final ThucHienBscRepository thucHienRepo;
    private final ThePhatHanhRepository thePhatHanhRepo;
    private final PhongBanRepository phongBanRepo;

    private static final BigDecimal BD0 = BigDecimal.ZERO;

    // Đúng thứ tự + nhãn 7 cột — cột i+1 trong Object[] kết quả (period ở cột 0) khớp index i.
    private static final String[][] METRIC_DEFS = {
        {"hdv-cuoi-ky", "HĐV CUỐI KỲ"},
        {"casa-bq",     "CASA BÌNH QUÂN"},
        {"du-no",       "DƯ NỢ"},
        {"tnt-dv",      "TNT TỪ DỊCH VỤ"},
        {"tnt-hdv",     "TNT TỪ HĐV"},
        {"tnt-td",      "TNT TỪ TÍN DỤNG"},
        {"tong-tnt",    "TỔNG TNT"},
    };

    @Override
    public AmChiTietResponse getChiTiet(List<ThongTinAm> amEntities, boolean isCanBo, String loaiKy, String selectedKy) {
        List<String> maAmCodes = maAmCodes(amEntities);

        List<Object[]> khRows = loadKeHoachAmRows(loaiKy, selectedKy);
        List<Object[]> thRows = loadThucHienAmRows(loaiKy, selectedKy);
        List<Object[]> prevRows = "ngay".equals(norm(loaiKy))
            ? List.of()
            : loadThucHienAmRows(loaiKy, calcPrevKy(loaiKy, selectedKy));

        BigDecimal[] kh = sumRowsForCodes(khRows, maAmCodes);
        BigDecimal[] th = sumRowsForCodes(thRows, maAmCodes);
        BigDecimal[] prev = sumRowsForCodes(prevRows, maAmCodes);

        long soThe = maAmCodes.isEmpty() ? 0 : thePhatHanhRepo.countByAmIssuingContractIn(maAmCodes);

        ThongTinAm rep = amEntities.get(0);
        Map<String, PhongBan> phongMap = buildPhongMap();
        PhongBan phong = rep.getMaDonViCap6() != null ? phongMap.get(rep.getMaDonViCap6()) : null;

        return AmChiTietResponse.builder()
            .maAmList(maAmCodes)
            .isCanBo(isCanBo)
            .tenAm(rep.getTenAm())
            .maDonViCap6(rep.getMaDonViCap6())
            .tenDonViCap6(phong != null ? phong.getTenDonViCap6() : null)
            .tenCn(phong != null ? phong.getTenCn() : null)
            .chucVu(rep.getChucVu())
            .email(rep.getEmail())
            .soDienThoai(rep.getSoDienThoai())
            .ngayBatDau(rep.getNgayBatDau())
            .trangThai(rep.getTrangThai())
            .soThe(soThe)
            .hdvCuoiKyTh(th[0]).hdvCuoiKyKh(kh[0]).hdvCuoiKyPct(pct(th[0], kh[0])).hdvCuoiKyDelta(delta(th[0], prev[0]))
            .casaBinhQuanTh(th[1]).casaBinhQuanKh(kh[1]).casaBinhQuanPct(pct(th[1], kh[1])).casaBinhQuanDelta(delta(th[1], prev[1]))
            .duNoTh(th[2]).duNoKh(kh[2]).duNoPct(pct(th[2], kh[2])).duNoDelta(delta(th[2], prev[2]))
            .tntDichVuTh(th[3]).tntDichVuKh(kh[3]).tntDichVuPct(pct(th[3], kh[3])).tntDichVuDelta(delta(th[3], prev[3]))
            .tntHdvTh(th[4]).tntHdvKh(kh[4]).tntHdvPct(pct(th[4], kh[4])).tntHdvDelta(delta(th[4], prev[4]))
            .tntTinDungTh(th[5]).tntTinDungKh(kh[5]).tntTinDungPct(pct(th[5], kh[5])).tntTinDungDelta(delta(th[5], prev[5]))
            .tntTh(th[6]).tntKh(kh[6]).tntPct(pct(th[6], kh[6])).tntDelta(delta(th[6], prev[6]))
            .build();
    }

    @Override
    public XuHuongResponse getXuHuong(List<ThongTinAm> amEntities, String loaiKy, int nam) {
        List<String> maAmCodes = maAmCodes(amEntities);
        String lk = norm(loaiKy);
        if ("ngay".equals(lk) || maAmCodes.isEmpty()) {
            return XuHuongResponse.builder().periods(List.of()).metrics(List.of()).build();
        }

        List<String> periods;
        List<Object[]> khRows;
        List<Object[]> thRows;

        switch (lk) {
            case "quy" -> {
                periods = List.of("Q1", "Q2", "Q3", "Q4");
                khRows = chiTieuRepo.keHoachTrendAmListByQuy(maAmCodes, nam);
                thRows = thucHienRepo.trendAmListByQuy(maAmCodes, nam);
            }
            case "nam" -> {
                List<Integer> years = List.of(nam - 3, nam - 2, nam - 1, nam);
                periods = years.stream().map(String::valueOf).collect(Collectors.toList());
                khRows = chiTieuRepo.keHoachTrendAmListByNam(maAmCodes, years);
                thRows = thucHienRepo.trendAmListByNam(maAmCodes, years);
            }
            default -> {
                periods = IntStream.rangeClosed(1, 12)
                    .mapToObj(m -> String.format("T%02d", m)).collect(Collectors.toList());
                khRows = chiTieuRepo.keHoachTrendAmListByThang(maAmCodes, nam);
                thRows = thucHienRepo.trendAmListByThang(maAmCodes, nam);
            }
        }

        Map<String, Object[]> khMap = keyByPeriod(khRows, lk);
        Map<String, Object[]> thMap = keyByPeriod(thRows, lk);

        List<XuHuongMetricSeries> metrics = new ArrayList<>();
        for (int i = 0; i < METRIC_DEFS.length; i++) {
            int col = i + 1;
            List<BigDecimal> khVals = new ArrayList<>();
            List<BigDecimal> thVals = new ArrayList<>();
            for (String p : periods) {
                khVals.add(valAt(khMap.get(p), col));
                thVals.add(valAt(thMap.get(p), col));
            }
            metrics.add(XuHuongMetricSeries.builder()
                .metricKey(METRIC_DEFS[i][0]).metricLabel(METRIC_DEFS[i][1])
                .keHoachValues(khVals).thucHienValues(thVals)
                .build());
        }

        return XuHuongResponse.builder().periods(periods).metrics(metrics).build();
    }

    // ── Data loaders (nguồn thực hiện AM luôn type_data=3 — xem amRowBy*/amDetailBy* trong
    // ThucHienBscRepository) ──────────────────────────────────────────────────────────────

    private List<Object[]> loadKeHoachAmRows(String loaiKy, String selectedKy) {
        return switch (norm(loaiKy)) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                String quy = "Q" + p[0].replace("Q", "");
                int nam = Integer.parseInt(p[1]);
                yield chiTieuRepo.keHoachAmByQuyNam(quy, nam);
            }
            case "nam", "ngay" -> chiTieuRepo.keHoachAmByNam(Integer.parseInt(selectedKy));
            default -> {
                String[] p = selectedKy.split("/");
                int thang = Integer.parseInt(p[0]);
                int nam = Integer.parseInt(p[1]);
                yield chiTieuRepo.keHoachAmByThangNam(thang, nam);
            }
        };
    }

    private List<Object[]> loadThucHienAmRows(String loaiKy, String selectedKy) {
        return switch (norm(loaiKy)) {
            case "ngay" -> thucHienRepo.amRowByNgayLatest();
            case "quy" -> {
                String[] p = selectedKy.split("/");
                String quy = "Q" + p[0].replace("Q", "");
                int nam = Integer.parseInt(p[1]);
                yield thucHienRepo.amRowByQuyNam(quy, nam);
            }
            case "nam" -> thucHienRepo.amRowByNam(Integer.parseInt(selectedKy));
            default -> {
                String[] p = selectedKy.split("/");
                int thang = Integer.parseInt(p[0]);
                int nam = Integer.parseInt(p[1]);
                yield thucHienRepo.amRowByThangNam(thang, nam);
            }
        };
    }

    private String calcPrevKy(String loaiKy, String selectedKy) {
        return switch (norm(loaiKy)) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                int q = Integer.parseInt(p[0].replace("Q", ""));
                int y = Integer.parseInt(p[1]);
                q--; if (q <= 0) { q = 4; y--; }
                yield "Q" + q + "/" + y;
            }
            case "nam" -> String.valueOf(Integer.parseInt(selectedKy) - 1);
            default -> {
                String[] p = selectedKy.split("/");
                int m = Integer.parseInt(p[0]);
                int y = Integer.parseInt(p[1]);
                m--; if (m <= 0) { m = 12; y--; }
                yield String.format("%02d/%d", m, y);
            }
        };
    }

    private BigDecimal[] sumRowsForCodes(List<Object[]> rows, List<String> codes) {
        BigDecimal[] sum = {BD0, BD0, BD0, BD0, BD0, BD0, BD0};
        Set<String> codeSet = new HashSet<>(codes);
        for (Object[] r : rows) {
            if (r[0] == null || !codeSet.contains(str(r[0]))) continue;
            for (int i = 0; i < 7; i++) sum[i] = sum[i].add(bd(r[i + 2]));
        }
        return sum;
    }

    // Chuẩn hoá period của từng dòng trend về đúng dạng chuỗi trong `periods` ("T01".."T12" /
    // "Q1".."Q4" / năm) để map theo key — giống hệt GiaoChiTieuServiceImpl.keyByPeriod.
    private Map<String, Object[]> keyByPeriod(List<Object[]> rows, String loaiKy) {
        return rows.stream().filter(r -> r[0] != null).collect(Collectors.toMap(
            r -> "thang".equals(norm(loaiKy)) ? String.format("T%02d", ((Number) r[0]).intValue()) : str(r[0]),
            r -> r, (a, b) -> a, LinkedHashMap::new));
    }

    private BigDecimal valAt(Object[] r, int col) {
        return r == null ? BD0 : bd(r[col]);
    }

    private List<String> maAmCodes(List<ThongTinAm> amEntities) {
        return amEntities.stream().map(ThongTinAm::getMaAm).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Map<String, PhongBan> buildPhongMap() {
        return phongBanRepo.findAll().stream()
            .filter(p -> p.getMaDonViCap6() != null)
            .collect(Collectors.toMap(PhongBan::getMaDonViCap6, p -> p, (a, b) -> a));
    }

    private String norm(String s) { return s == null ? "thang" : s; }
    private String str(Object v)  { return v == null ? "" : v.toString(); }

    private BigDecimal bd(Object v) {
        if (v == null) return BD0;
        if (v instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }

    private Double pct(BigDecimal th, BigDecimal kh) {
        if (kh == null || kh.compareTo(BD0) == 0) return null;
        return th.divide(kh.abs(), 4, RoundingMode.HALF_UP)
                 .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    private Double delta(BigDecimal cur, BigDecimal prev) {
        if (prev == null || prev.compareTo(BD0) == 0) return null;
        return cur.subtract(prev)
                  .divide(prev.abs(), 4, RoundingMode.HALF_UP)
                  .multiply(BigDecimal.valueOf(100)).doubleValue();
    }
}
