package com.mpa.service.impl;

import com.mpa.dto.AmDetailResponse;
import com.mpa.dto.DashboardKpiResponse;
import com.mpa.dto.KpiSumResult;
import com.mpa.dto.PhongDataResponse;
import com.mpa.dto.PhongTrendSeriesResponse;
import com.mpa.dto.TrendDataResponse;
import com.mpa.repository.ThucHienBscRepository;
import com.mpa.repository.DuLieuMpaRepository;
import com.mpa.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DuLieuMpaRepository mpaRepo;   // thực tế → du_lieu_mpa
    private final ThucHienBscRepository bscRepo;  // thực hiện/kế hoạch → thuc_hien_bsc_chi_nhanh

    // ── KPI ────────────────────────────────────────────────────────

    @Override
    public DashboardKpiResponse getKpi(String loaiKy, String selectedKy, String soVoi) {
        KpiSumResult current, prev, kh;

        // current & prev đều lấy từ thuc_hien_bsc_chi_nhanh (type_data=1)
        // kh (BSC năm) lấy từ thuc_hien_bsc_chi_nhanh (type_data=0)
        switch (loaiKy == null ? "thang" : loaiKy) {
            case "quy" -> {
                String[] parts = selectedKy.split("/");
                int q = Integer.parseInt(parts[0].replace("Q", ""));
                int y = Integer.parseInt(parts[1]);
                String quyStr = "Q" + q;
                int prevQ = q - 1, prevY = y;
                if (prevQ <= 0) { prevQ = 4; prevY = y - 1; }
                if ("dau-nam".equals(soVoi)) { prevQ = 1; prevY = y; }
                String prevQuyStr = "Q" + prevQ;

                current = bscRepo.sumThucHienByQuyNam(quyStr, y);
                prev    = bscRepo.sumThucHienByQuyNam(prevQuyStr, prevY);
                kh      = bscRepo.sumKhByNam(y);
            }
            case "nam" -> {
                int y = Integer.parseInt(selectedKy);
                current = bscRepo.sumThucHienByNam(y);
                prev    = bscRepo.sumThucHienByNam(y - 1);
                kh      = bscRepo.sumKhByNam(y);
            }
            case "ngay" -> {
                // BSC không có dữ liệu theo ngày → dùng tháng của ngày được chọn
                LocalDate ngay = LocalDate.parse(selectedKy);
                int m = ngay.getMonthValue(), y = ngay.getYear();
                int prevM = m - 1, prevY = y;
                if (prevM <= 0) { prevM = 12; prevY--; }
                current = bscRepo.sumThucHienByThangNam(m, y);
                prev    = bscRepo.sumThucHienByThangNam(prevM, prevY);
                kh      = bscRepo.sumKhByNam(y);
            }
            default -> {  // thang
                String[] parts = selectedKy.split("/");
                int m = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int prevM = m - 1, prevY = y;
                if (prevM <= 0) { prevM = 12; prevY = y - 1; }
                if ("dau-nam".equals(soVoi))        { prevM = 1; prevY = y; }
                else if ("quy-truoc".equals(soVoi)) {
                    prevM -= 3;
                    if (prevM <= 0) { prevM += 12; prevY = y - 1; }
                }
                current = bscRepo.sumThucHienByThangNam(m, y);
                prev    = bscRepo.sumThucHienByThangNam(prevM, prevY);
                kh      = bscRepo.sumKhByNam(y);
            }
        }

        return buildKpiResponse(current, prev, kh);
    }

    // ── Trend ──────────────────────────────────────────────────────

    @Override
    public TrendDataResponse getTrend(String loaiKy, String selectedKy, String metricKey) {
        int mIdx = metricIndex(metricKey);

        // Dữ liệu trend lấy từ thuc_hien_bsc_chi_nhanh (type_data=1)
        return switch (loaiKy == null ? "thang" : loaiKy) {
            case "quy" -> {
                int y = Integer.parseInt(selectedKy.split("/")[1]);
                String[] quyLabels = {"Q1", "Q2", "Q3", "Q4"};
                Map<String, BigDecimal> cur = toStrMap(bscRepo.trendAllByQuy(y), mIdx);
                Map<String, BigDecimal> prv = toStrMap(bscRepo.trendAllByQuy(y - 1), mIdx);
                yield TrendDataResponse.builder()
                        .currentYear(y).prevYear(y - 1)
                        .labels(Arrays.asList(quyLabels))
                        .currentValues(mapQuy(quyLabels, cur))
                        .prevValues(mapQuy(quyLabels, prv))
                        .build();
            }
            case "nam" -> {
                int y = Integer.parseInt(selectedKy);
                List<Integer> years = Arrays.asList(y - 3, y - 2, y - 1, y);
                Map<Integer, BigDecimal> dat = toIntMap(bscRepo.trendAllByNam(years), mIdx);
                yield TrendDataResponse.builder()
                        .currentYear(y).prevYear(0)
                        .labels(years.stream().map(String::valueOf).collect(Collectors.toList()))
                        .currentValues(years.stream().map(yr -> dat.getOrDefault(yr, BigDecimal.ZERO)).collect(Collectors.toList()))
                        .prevValues(Collections.emptyList())
                        .build();
            }
            case "ngay" -> {
                LocalDate d = LocalDate.parse(selectedKy);
                int m = d.getMonthValue(), y = d.getYear();
                LocalDate prevFirst = d.withDayOfMonth(1).minusMonths(1);
                int pm = prevFirst.getMonthValue(), py = prevFirst.getYear();
                int days = YearMonth.of(y, m).lengthOfMonth();
                Map<Integer, BigDecimal> cur = toNgayMap(bscRepo.trendAllByNgay(m, y), mIdx);
                Map<Integer, BigDecimal> prv = toNgayMap(bscRepo.trendAllByNgay(pm, py), mIdx);
                List<String> labels = IntStream.rangeClosed(1, days)
                        .mapToObj(day -> String.format("%02d", day)).collect(Collectors.toList());
                yield TrendDataResponse.builder()
                        .currentYear(y).prevYear(py * 100 + pm)
                        .labels(labels)
                        .currentValues(IntStream.rangeClosed(1, days)
                                .mapToObj(day -> cur.getOrDefault(day, BigDecimal.ZERO)).collect(Collectors.toList()))
                        .prevValues(IntStream.rangeClosed(1, days)
                                .mapToObj(day -> prv.getOrDefault(day, BigDecimal.ZERO)).collect(Collectors.toList()))
                        .build();
            }
            default -> {  // thang
                int y = Integer.parseInt(selectedKy.split("/")[1]);
                Map<Integer, BigDecimal> cur = toIntMap(bscRepo.trendAllByThang(y), mIdx);
                Map<Integer, BigDecimal> prv = toIntMap(bscRepo.trendAllByThang(y - 1), mIdx);
                List<String> labels = IntStream.rangeClosed(1, 12)
                        .mapToObj(mo -> "T" + String.format("%02d", mo)).collect(Collectors.toList());
                yield TrendDataResponse.builder()
                        .currentYear(y).prevYear(y - 1)
                        .labels(labels)
                        .currentValues(IntStream.rangeClosed(1, 12)
                                .mapToObj(mo -> cur.getOrDefault(mo, BigDecimal.ZERO)).collect(Collectors.toList()))
                        .prevValues(IntStream.rangeClosed(1, 12)
                                .mapToObj(mo -> prv.getOrDefault(mo, BigDecimal.ZERO)).collect(Collectors.toList()))
                        .build();
            }
        };
    }

    // ── Phong Data ─────────────────────────────────────────────────
    // Metrics: thuc_hien_bsc_chi_nhanh (type_data=2)
    // soAm:        thuc_hien_bsc_chi_nhanh (type_data=3, COUNT(*) per ma_don_vi_cap_6 for the period)
    // soKhachHang: du_lieu_mpa (COUNT(*) per ma_don_vi_cap_6 for the period)

    @Override
    public List<PhongDataResponse> getPhongData(String loaiKy, String selectedKy, String soVoi) {
        List<Object[]> curRows = bscPhongCurrent(loaiKy, selectedKy);
        List<Object[]> prvRows = bscPhongPrev(loaiKy, selectedKy, soVoi);
        List<Object[]> amRows  = bscAmCount(loaiKy, selectedKy);
        List<Object[]> khRows  = mpaAmKh(loaiKy, selectedKy);

        Map<String, Object[]> prevMap = prvRows.stream().collect(
            Collectors.toMap(r -> (String) r[0], r -> r, (a, b) -> a));
        Map<String, Integer> amMap = amRows.stream().collect(
            Collectors.toMap(r -> (String) r[0],
                r -> ((Number) r[1]).intValue(),
                (a, b) -> a));
        // MPA: keyed by maDonViCap6 → soKhachHang (COUNT(*))
        Map<String, Integer> khMap = khRows.stream().collect(
            Collectors.toMap(r -> (String) r[0],
                r -> ((Number) r[1]).intValue(),
                (a, b) -> a));

        // BSC column order: [maDonViCap6, tenDonViCap6, hdvCuoiKy, casa, duNo, tntDv, tntHdv, tntTd, tnt]
        return curRows.stream().map(r -> {
            String ma    = (String) r[0];
            String tenDv = (String) r[1];
            Object[] prv = prevMap.get(ma);
            int soAm     = amMap.getOrDefault(tenDv, 0);
            int soKh     = khMap.getOrDefault(ma, 0);

            BigDecimal curHdvCk  = toBd(r[2]);
            BigDecimal curCasa   = toBd(r[3]);
            BigDecimal curDuNo   = toBd(r[4]);
            BigDecimal curTntDv  = toBd(r[5]);
            BigDecimal curTntHdv = toBd(r[6]);
            BigDecimal curTntTd  = toBd(r[7]);
            BigDecimal curTnt    = toBd(r[8]);

            BigDecimal prvHdvCk  = prv != null ? toBd(prv[2]) : null;
            BigDecimal prvCasa   = prv != null ? toBd(prv[3]) : null;
            BigDecimal prvDuNo   = prv != null ? toBd(prv[4]) : null;
            BigDecimal prvTntDv  = prv != null ? toBd(prv[5]) : null;
            BigDecimal prvTntHdv = prv != null ? toBd(prv[6]) : null;
            BigDecimal prvTntTd  = prv != null ? toBd(prv[7]) : null;
            BigDecimal prvTnt    = prv != null ? toBd(prv[8]) : null;

            return PhongDataResponse.builder()
                    .maDonViCap6(ma)
                    .tenDonViCap6((String) r[1])
                    .thuNhapThuan(curTnt)
                    .thuNhapThuanPrevious(prvTnt != null ? prvTnt : BigDecimal.ZERO)
                    .hdvCuoiKy(curHdvCk)
                    .hdvBinhQuan(curCasa)
                    .duNo(curDuNo)
                    .thuNhapThuanDichVu(curTntDv)
                    .thuNhapThuanHdvFtp(curTntHdv)
                    .thuNhapThuanTinDung(curTntTd)
                    .soAm(soAm)
                    .soKhachHang(soKh)
                    .changePercent(pctChange(curTnt, prvTnt))
                    .hdvCuoiKyChangePercent(pctChange(curHdvCk, prvHdvCk))
                    .hdvBinhQuanChangePercent(pctChange(curCasa, prvCasa))
                    .duNoChangePercent(pctChange(curDuNo, prvDuNo))
                    .tntDichVuChangePercent(pctChange(curTntDv, prvTntDv))
                    .tntHdvChangePercent(pctChange(curTntHdv, prvTntHdv))
                    .tntTinDungChangePercent(pctChange(curTntTd, prvTntTd))
                    .build();
        }).collect(Collectors.toList());
    }

    private List<Object[]> bscPhongCurrent(String loaiKy, String selectedKy) {
        return switch (loaiKy == null ? "thang" : loaiKy) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                yield bscRepo.phongTableByQuyNam("Q" + p[0].replace("Q",""), Integer.parseInt(p[1]));
            }
            case "nam" -> bscRepo.phongTableByNam(Integer.parseInt(selectedKy));
            default -> {
                int m, y;
                if ("ngay".equals(loaiKy)) {
                    LocalDate d = LocalDate.parse(selectedKy); m = d.getMonthValue(); y = d.getYear();
                } else {
                    String[] p = selectedKy.split("/"); m = Integer.parseInt(p[0]); y = Integer.parseInt(p[1]);
                }
                yield bscRepo.phongTableByThangNam(m, y);
            }
        };
    }

    private List<Object[]> bscPhongPrev(String loaiKy, String selectedKy, String soVoi) {
        return switch (loaiKy == null ? "thang" : loaiKy) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                int q = Integer.parseInt(p[0].replace("Q","")), y = Integer.parseInt(p[1]);
                int pq = q - 1, py = y; if (pq <= 0) { pq = 4; py--; }
                if ("dau-nam".equals(soVoi)) { pq = 1; py = y; }
                yield bscRepo.phongTableByQuyNam("Q" + pq, py);
            }
            case "nam" -> bscRepo.phongTableByNam(Integer.parseInt(selectedKy) - 1);
            default -> {
                int m, y;
                if ("ngay".equals(loaiKy)) {
                    LocalDate d = LocalDate.parse(selectedKy); m = d.getMonthValue(); y = d.getYear();
                } else {
                    String[] p = selectedKy.split("/"); m = Integer.parseInt(p[0]); y = Integer.parseInt(p[1]);
                }
                int pm = m - 1, py = y; if (pm <= 0) { pm = 12; py--; }
                if ("dau-nam".equals(soVoi)) { pm = 1; py = y; }
                else if ("quy-truoc".equals(soVoi)) { pm -= 3; if (pm <= 0) { pm += 12; py--; } }
                yield bscRepo.phongTableByThangNam(pm, py);
            }
        };
    }

    private List<Object[]> bscAmCount(String loaiKy, String selectedKy) {
        return switch (loaiKy == null ? "thang" : loaiKy) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                yield bscRepo.phongAmCountByQuyNam("Q" + p[0].replace("Q",""), Integer.parseInt(p[1]));
            }
            case "nam" -> bscRepo.phongAmCountByNam(Integer.parseInt(selectedKy));
            default -> {
                int m, y;
                if ("ngay".equals(loaiKy)) {
                    LocalDate d = LocalDate.parse(selectedKy); m = d.getMonthValue(); y = d.getYear();
                } else {
                    String[] p = selectedKy.split("/"); m = Integer.parseInt(p[0]); y = Integer.parseInt(p[1]);
                }
                yield bscRepo.phongAmCountByThangNam(m, y);
            }
        };
    }

    private List<Object[]> mpaAmKh(String loaiKy, String selectedKy) {
        return switch (loaiKy == null ? "thang" : loaiKy) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                yield mpaRepo.phongKhCountByQuyNam("Q" + p[0].replace("Q",""), Integer.parseInt(p[1]));
            }
            case "nam" -> mpaRepo.phongKhCountByNam(Integer.parseInt(selectedKy));
            default -> {
                int m, y;
                if ("ngay".equals(loaiKy)) {
                    LocalDate d = LocalDate.parse(selectedKy); m = d.getMonthValue(); y = d.getYear();
                } else {
                    String[] p = selectedKy.split("/"); m = Integer.parseInt(p[0]); y = Integer.parseInt(p[1]);
                }
                yield mpaRepo.phongKhCountByThangNam(m, y);
            }
        };
    }

    // ── AM Detail (chi_tieu_bsc, type_data=3) ──────────────────────

    @Override
    public List<AmDetailResponse> getAmDetail(String loaiKy, String selectedKy, String maDonViCap6, String soVoi) {
        List<Object[]> curRows = amDetailCurrent(loaiKy, selectedKy, maDonViCap6);
        List<Object[]> prvRows = amDetailPrev(loaiKy, selectedKy, soVoi, maDonViCap6);

        // Map kỳ trước theo maAm để tra nhanh
        // Object[]: [maAm, tenAm, hdvCuoiKy, casa, duNo, tntDv, tntHdv, tntTd, tnt]
        Map<String, Object[]> prvMap = prvRows.stream()
                .filter(r -> r[0] != null)
                .collect(Collectors.toMap(r -> r[0].toString(), r -> r, (a, b) -> a));

        return curRows.stream().map(r -> {
            String ma    = r[0] != null ? r[0].toString() : "";
            String tenAm = r[1] != null ? r[1].toString() : ma;
            Object[] prv = prvMap.get(ma);

            BigDecimal curHdvCk  = toBd(r[2]);
            BigDecimal curCasa   = toBd(r[3]);
            BigDecimal curDuNo   = toBd(r[4]);
            BigDecimal curTntDv  = toBd(r[5]);
            BigDecimal curTntHdv = toBd(r[6]);
            BigDecimal curTntTd  = toBd(r[7]);
            BigDecimal curTnt    = toBd(r[8]);

            BigDecimal prvHdvCk  = prv != null ? toBd(prv[2]) : null;
            BigDecimal prvCasa   = prv != null ? toBd(prv[3]) : null;
            BigDecimal prvDuNo   = prv != null ? toBd(prv[4]) : null;
            BigDecimal prvTntDv  = prv != null ? toBd(prv[5]) : null;
            BigDecimal prvTntHdv = prv != null ? toBd(prv[6]) : null;
            BigDecimal prvTntTd  = prv != null ? toBd(prv[7]) : null;
            BigDecimal prvTnt    = prv != null ? toBd(prv[8]) : null;

            return AmDetailResponse.builder()
                    .maAm(ma)
                    .tenAm(tenAm)
                    .tenDonViCap6(maDonViCap6)
                    .hdvCuoiKy(curHdvCk)
                    .hdvBinhQuan(curCasa)
                    .duNo(curDuNo)
                    .thuNhapThuanDichVu(curTntDv)
                    .thuNhapThuanHdvFtp(curTntHdv)
                    .thuNhapThuanTinDung(curTntTd)
                    .thuNhapThuan(curTnt)
                    .hdvCuoiKyChangePct(pctChange(curHdvCk, prvHdvCk))
                    .hdvBinhQuanChangePct(pctChange(curCasa, prvCasa))
                    .duNoChangePct(pctChange(curDuNo, prvDuNo))
                    .tntDichVuChangePct(pctChange(curTntDv, prvTntDv))
                    .tntHdvChangePct(pctChange(curTntHdv, prvTntHdv))
                    .tntTinDungChangePct(pctChange(curTntTd, prvTntTd))
                    .thuNhapThuanChangePct(pctChange(curTnt, prvTnt))
                    .build();
        }).collect(Collectors.toList());
    }

    private List<Object[]> amDetailCurrent(String loaiKy, String selectedKy, String maDonViCap6) {
        return switch (loaiKy == null ? "thang" : loaiKy) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                yield bscRepo.amDetailByQuyNam(maDonViCap6, "Q" + p[0].replace("Q", ""), Integer.parseInt(p[1]));
            }
            case "nam" -> bscRepo.amDetailByNam(maDonViCap6, Integer.parseInt(selectedKy));
            default -> {
                int m, y;
                if ("ngay".equals(loaiKy)) {
                    LocalDate d = LocalDate.parse(selectedKy); m = d.getMonthValue(); y = d.getYear();
                } else {
                    String[] p = selectedKy.split("/"); m = Integer.parseInt(p[0]); y = Integer.parseInt(p[1]);
                }
                yield bscRepo.amDetailByThangNam(maDonViCap6, m, y);
            }
        };
    }

    private List<Object[]> amDetailPrev(String loaiKy, String selectedKy, String soVoi, String maDonViCap6) {
        return switch (loaiKy == null ? "thang" : loaiKy) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                int q = Integer.parseInt(p[0].replace("Q", "")), y = Integer.parseInt(p[1]);
                int pq = q - 1, py = y; if (pq <= 0) { pq = 4; py--; }
                if ("dau-nam".equals(soVoi)) { pq = 1; py = y; }
                yield bscRepo.amDetailByQuyNam(maDonViCap6, "Q" + pq, py);
            }
            case "nam" -> bscRepo.amDetailByNam(maDonViCap6, Integer.parseInt(selectedKy) - 1);
            default -> {
                int m, y;
                if ("ngay".equals(loaiKy)) {
                    LocalDate d = LocalDate.parse(selectedKy); m = d.getMonthValue(); y = d.getYear();
                } else {
                    String[] p = selectedKy.split("/"); m = Integer.parseInt(p[0]); y = Integer.parseInt(p[1]);
                }
                int pm = m - 1, py = y; if (pm <= 0) { pm = 12; py--; }
                if ("dau-nam".equals(soVoi))        { pm = 1; py = y; }
                else if ("quy-truoc".equals(soVoi)) { pm -= 3; if (pm <= 0) { pm += 12; py--; } }
                yield bscRepo.amDetailByThangNam(maDonViCap6, pm, py);
            }
        };
    }

    // ── Phong Trend (chi_tieu_bsc, type_data=2) ────────────────────

    @Override
    public List<PhongTrendSeriesResponse> getPhongTrend(String loaiKy, String selectedKy, String metricKey) {
        int mIdx = phongTrendMetricIdx(metricKey);

        return switch (loaiKy == null ? "thang" : loaiKy) {
            case "quy" -> {
                int y = Integer.parseInt(selectedKy.split("/")[1]);
                String[] labels = {"Q1", "Q2", "Q3", "Q4"};
                yield buildPhongTrendSeries(bscRepo.phongTrendByQuy(y), labels, mIdx,
                    r -> (String) r[1]);
            }
            case "nam" -> {
                int y = Integer.parseInt(selectedKy);
                List<Integer> years = Arrays.asList(y - 3, y - 2, y - 1, y);
                String[] labels = years.stream().map(String::valueOf).toArray(String[]::new);
                yield buildPhongTrendSeries(bscRepo.phongTrendByNam(years), labels, mIdx,
                    r -> String.valueOf(((Number) r[1]).intValue()));
            }
            default -> {
                int y = "ngay".equals(loaiKy)
                    ? LocalDate.parse(selectedKy).getYear()
                    : Integer.parseInt(selectedKy.split("/")[1]);
                String[] labels = IntStream.rangeClosed(1, 12)
                    .mapToObj(m -> "T" + String.format("%02d", m)).toArray(String[]::new);
                yield buildPhongTrendSeries(bscRepo.phongTrendByThang(y), labels, mIdx,
                    r -> r[1] == null ? null : "T" + String.format("%02d", ((Number) r[1]).intValue()));
            }
        };
    }

    private int phongTrendMetricIdx(String key) {
        return switch (key == null ? "" : key) {
            case "hdv-cuoi-ky"  -> 2;
            case "casa"         -> 3;
            case "du-no"        -> 4;
            case "tnt-dich-vu"  -> 5;
            case "tnt-hdv"      -> 6;
            case "tnt-tin-dung" -> 7;
            default             -> 8; // tong-tnt
        };
    }

    private List<PhongTrendSeriesResponse> buildPhongTrendSeries(
            List<Object[]> rows, String[] labels, int mIdx,
            Function<Object[], String> periodKeyFn) {

        Map<String, Map<String, BigDecimal>> phongMap = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String ten = r[0] != null ? (String) r[0] : "(Không rõ)";
            String period = periodKeyFn.apply(r);
            if (period == null) continue;
            phongMap.computeIfAbsent(ten, k -> new HashMap<>())
                    .merge(period, toBd(r[mIdx]), BigDecimal::add);
        }

        return phongMap.entrySet().stream()
            .map(e -> {
                BigDecimal total = e.getValue().values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                List<BigDecimal> vals = Arrays.stream(labels)
                        .map(l -> e.getValue().getOrDefault(l, BigDecimal.ZERO))
                        .collect(Collectors.toList());
                return new AbstractMap.SimpleEntry<>(total,
                    PhongTrendSeriesResponse.builder()
                        .tenDonViCap6(e.getKey())
                        .values(vals)
                        .build());
            })
            .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
            .limit(5)
            .map(AbstractMap.SimpleEntry::getValue)
            .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────

    private int metricIndex(String key) {
        return switch (key == null ? "" : key) {
            case "hdv-cuoi-ky"  -> 1;
            case "casa"         -> 2;
            case "du-no"        -> 3;
            case "tnt-dich-vu"  -> 4;
            case "tnt-hdv"      -> 5;
            case "tnt-tin-dung" -> 6;
            default             -> 7; // tong-tnt
        };
    }

    private Map<Integer, BigDecimal> toIntMap(List<Object[]> rows, int idx) {
        return rows.stream()
            .filter(r -> r[0] != null)
            .collect(Collectors.toMap(
                r -> ((Number) r[0]).intValue(),
                r -> r[idx] == null ? BigDecimal.ZERO : (BigDecimal) r[idx],
                (a, b) -> a
            ));
    }

    private Map<String, BigDecimal> toStrMap(List<Object[]> rows, int idx) {
        return rows.stream()
            .filter(r -> r[0] != null)
            .collect(Collectors.toMap(
                r -> (String) r[0],
                r -> r[idx] == null ? BigDecimal.ZERO : (BigDecimal) r[idx],
                (a, b) -> a
            ));
    }

    private Map<Integer, BigDecimal> toNgayMap(List<Object[]> rows, int idx) {
        return rows.stream()
            .filter(r -> r[0] != null)
            .collect(Collectors.toMap(
                r -> ((LocalDate) r[0]).getDayOfMonth(),
                r -> r[idx] == null ? BigDecimal.ZERO : (BigDecimal) r[idx],
                (a, b) -> a
            ));
    }

    private List<BigDecimal> mapQuy(String[] labels, Map<String, BigDecimal> map) {
        return Arrays.stream(labels).map(q -> map.getOrDefault(q, BigDecimal.ZERO)).collect(Collectors.toList());
    }

    private BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }

    private DashboardKpiResponse buildKpiResponse(KpiSumResult cur, KpiSumResult prv, KpiSumResult kh) {
        return DashboardKpiResponse.builder()
                .tongHdvCuoiKy(safe(cur.getHuyDongVonCuoiKy()))
                .tongHdvBinhQuan(safe(cur.getCasaBinhQuan()))
                .tongDuNo(safe(cur.getDuNoTinDungCuoiKy()))
                .tongTntDichVu(safe(cur.getThuNhapThuanDichVu()))
                .tongTntHdvFtp(safe(cur.getThuNhapThuanHdvFtp()))
                .tongTntTinDung(safe(cur.getThuNhapThuanTinDung()))
                .tongThuNhapThuan(safe(cur.getThuNhapThuan()))
                .tongHdvCuoiKyPrev(safe(prv.getHuyDongVonCuoiKy()))
                .tongHdvBinhQuanPrev(safe(prv.getCasaBinhQuan()))
                .tongDuNoPrev(safe(prv.getDuNoTinDungCuoiKy()))
                .tongTntDichVuPrev(safe(prv.getThuNhapThuanDichVu()))
                .tongTntHdvFtpPrev(safe(prv.getThuNhapThuanHdvFtp()))
                .tongTntTinDungPrev(safe(prv.getThuNhapThuanTinDung()))
                .tongThuNhapThuanPrev(safe(prv.getThuNhapThuan()))
                .khHdvCuoiKy(safe(kh.getHuyDongVonCuoiKy()))
                .khHdvBinhQuan(safe(kh.getCasaBinhQuan()))
                .khDuNo(safe(kh.getDuNoTinDungCuoiKy()))
                .khTntDichVu(safe(kh.getThuNhapThuanDichVu()))
                .khTntHdvFtp(safe(kh.getThuNhapThuanHdvFtp()))
                .khTntTinDung(safe(kh.getThuNhapThuanTinDung()))
                .khTnt(safe(kh.getThuNhapThuan()))
                .soKhachHang(0).soAm(0).soPhong(0)
                .build();
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Double pctChange(BigDecimal cur, BigDecimal prv) {
        if (prv == null) return null;
        if (prv.compareTo(BigDecimal.ZERO) == 0) return null;
        return cur.subtract(prv)
                  .divide(prv.abs(), 4, RoundingMode.HALF_UP)
                  .multiply(BigDecimal.valueOf(100))
                  .doubleValue();
    }
}
