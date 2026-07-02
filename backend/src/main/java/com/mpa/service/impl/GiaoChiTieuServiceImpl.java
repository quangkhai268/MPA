package com.mpa.service.impl;

import com.mpa.dto.BscSoSanhResponse;
import com.mpa.dto.BscSoSanhRowResponse;
import com.mpa.dto.ChiTieuBscRequest;
import com.mpa.dto.ChiTieuQuanLyRow;
import com.mpa.entity.ChiTieuBscChiNhanh;
import com.mpa.repository.ChiNhanhRepository;
import com.mpa.repository.ChiTieuBscRepository;
import com.mpa.repository.DuLieuMpaRepository;
import com.mpa.repository.PhongBanRepository;
import com.mpa.service.GiaoChiTieuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GiaoChiTieuServiceImpl implements GiaoChiTieuService {

    private final ChiTieuBscRepository repo;
    private final DuLieuMpaRepository duLieuMpaRepo;
    private final PhongBanRepository phongBanRepo;
    private final ChiNhanhRepository chiNhanhRepo;

    @Override
    public BscSoSanhResponse getSoSanh(String loaiKy, String selectedKy, String doiTuong) {
        int nam = parseNam(loaiKy, selectedKy);

        List<Object[]> khRows   = loadKeHoach(doiTuong, nam);
        List<Object[]> thRows   = loadThucHien(loaiKy, selectedKy, doiTuong);
        List<Object[]> prevRows = loadThucHien(loaiKy, calcPrevKy(loaiKy, selectedKy), doiTuong);

        Map<String, Object[]> khMap   = toMap(khRows);
        Map<String, Object[]> thMap   = toMap(thRows);
        Map<String, Object[]> prevMap = toMap(prevRows);

        Set<String> allKeys = new LinkedHashSet<>();
        thRows.stream().map(r -> str(r[0])).forEach(allKeys::add);
        khRows.stream().map(r -> str(r[0])).forEach(allKeys::add);

        int dat = 0, canh = 0, rui = 0, chuaGiao = 0;
        List<BscSoSanhRowResponse> rows = new ArrayList<>();

        for (String key : allKeys) {
            BigDecimal[] thV   = extractVals(thMap.get(key));
            BigDecimal[] khV   = extractVals(khMap.get(key));
            BigDecimal[] prevV = extractVals(prevMap.get(key));

            String name = thMap.containsKey(key) ? str(thMap.get(key)[1])
                        : khMap.containsKey(key) ? str(khMap.get(key)[1]) : key;

            for (int i = 0; i < 7; i++) {
                if (khV[i].compareTo(BigDecimal.ZERO) == 0) {
                    chuaGiao++;
                } else {
                    Double p = pct(thV[i], khV[i]);
                    if (p == null || p < 80) rui++;
                    else if (p < 100) canh++;
                    else dat++;
                }
            }

            rows.add(BscSoSanhRowResponse.builder()
                .maUnit(key).tenUnit(name)
                .hdvCuoiKyTh(thV[0]).hdvCuoiKyKh(khV[0])
                    .hdvCuoiKyPct(pct(thV[0], khV[0])).hdvCuoiKyDelta(delta(thV[0], prevV[0]))
                .casaBinhQuanTh(thV[1]).casaBinhQuanKh(khV[1])
                    .casaBinhQuanPct(pct(thV[1], khV[1])).casaBinhQuanDelta(delta(thV[1], prevV[1]))
                .duNoTh(thV[2]).duNoKh(khV[2])
                    .duNoPct(pct(thV[2], khV[2])).duNoDelta(delta(thV[2], prevV[2]))
                .tntDichVuTh(thV[3]).tntDichVuKh(khV[3])
                    .tntDichVuPct(pct(thV[3], khV[3])).tntDichVuDelta(delta(thV[3], prevV[3]))
                .tntHdvTh(thV[4]).tntHdvKh(khV[4])
                    .tntHdvPct(pct(thV[4], khV[4])).tntHdvDelta(delta(thV[4], prevV[4]))
                .tntTinDungTh(thV[5]).tntTinDungKh(khV[5])
                    .tntTinDungPct(pct(thV[5], khV[5])).tntTinDungDelta(delta(thV[5], prevV[5]))
                .tntTh(thV[6]).tntKh(khV[6])
                    .tntPct(pct(thV[6], khV[6])).tntDelta(delta(thV[6], prevV[6]))
                .build());
        }

        return BscSoSanhResponse.builder()
            .datKeHoach(dat).canhBao(canh).ruiRo(rui).chuaGiao(chuaGiao)
            .total(dat + canh + rui + chuaGiao)
            .rows(rows)
            .build();
    }

    // ── THÊM / XÓA CHỈ TIÊU ────────────────────────────────────────

    @Override
    public void themChiTieu(ChiTieuBscRequest req) {
        int nam = parseNam(req.getLoaiKy(), req.getSelectedKy());
        ChiTieuBscChiNhanh entity = switch (norm(req.getDoiTuong())) {
            case "chi-nhanh" -> repo.findKeHoachCn(nam, req.getMaUnit())
                .orElseGet(() -> newCn(req, nam));
            case "am" -> repo.findKeHoachAm(nam, req.getMaUnit())
                .orElseGet(() -> newAm(req, nam));
            default -> repo.findKeHoachPhong(nam, req.getMaUnit())
                .orElseGet(() -> newPhong(req, nam));
        };
        applyKpi(entity, req.getChiTieu(), req.getMucTieu());
        entity.setNgayTao(LocalDateTime.now());
        repo.save(entity);
    }

    @Override
    public void deleteChiTieu(Integer id) {
        repo.deleteById(id);
    }

    // ── QUẢN LÝ LIST ────────────────────────────────────────────────

    @Override
    public List<ChiTieuQuanLyRow> getQuanLyList(String loaiKy, String selectedKy, String doiTuong) {
        int nam = parseNam(loaiKy, selectedKy);
        List<ChiTieuBscChiNhanh> list = switch (norm(doiTuong)) {
            case "chi-nhanh" -> repo.findKeHoachCnList(nam);
            case "am"        -> repo.findKeHoachAmList(nam);
            default          -> repo.findKeHoachPhongList(nam);
        };
        return list.stream().map(c -> {
            String maUnit  = switch (norm(doiTuong)) {
                case "chi-nhanh" -> c.getMaCn();
                case "am"        -> c.getMaAm();
                default          -> c.getMaDonViCap6();
            };
            String tenUnit = switch (norm(doiTuong)) {
                case "chi-nhanh" -> c.getTenCn();
                case "am"        -> c.getTenAm();
                default          -> c.getTenDonViCap6();
            };
            return ChiTieuQuanLyRow.builder()
                .id(c.getId())
                .maUnit(maUnit)
                .tenUnit(tenUnit)
                .hdvCuoiKy(c.getHuyDongVonCuoiKy())
                .casaBinhQuan(c.getCasaBinhQuan())
                .duNoTinDung(c.getDuNoTinDungCuoiKy())
                .tntDichVu(c.getThuNhapThuanDichVu())
                .tntHdvFtp(c.getThuNhapThuanHdvFtp())
                .tntTinDung(c.getThuNhapThuanTinDung())
                .thuNhapThuan(c.getThuNhapThuan())
                .ngayTao(c.getNgayTao())
                .build();
        }).collect(Collectors.toList());
    }

    // ── DROPDOWN LISTS ──────────────────────────────────────────────

    @Override
    public List<Map<String, String>> getPhongList() {
        return phongBanRepo.findAllActive().stream()
            .map(p -> Map.of("ma", p.getMaDonViCap6(), "ten", p.getTenDonViCap6()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> getCnList() {
        return chiNhanhRepo.findAllActive().stream()
            .map(c -> Map.of("ma", c.getMaCn(), "ten", c.getTenCn()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> getAmList() {
        return duLieuMpaRepo.findDistinctAmList().stream()
            .map(r -> Map.of("ma", str(r[0]), "ten", str(r[1])))
            .collect(Collectors.toList());
    }

    // ── KPI apply ──────────────────────────────────────────────────

    private void applyKpi(ChiTieuBscChiNhanh e, String chiTieu, BigDecimal val) {
        if (val == null) val = BigDecimal.ZERO;
        switch (chiTieu) {
            case "hdv-cuoi-ky" -> e.setHuyDongVonCuoiKy(val);
            case "casa-bq"     -> e.setCasaBinhQuan(val);
            case "du-no"       -> e.setDuNoTinDungCuoiKy(val);
            case "tnt-dv"      -> e.setThuNhapThuanDichVu(val);
            case "tnt-hdv"     -> e.setThuNhapThuanHdvFtp(val);
            case "tnt-td"      -> e.setThuNhapThuanTinDung(val);
            default            -> e.setThuNhapThuan(val); // "tong-tnt"
        }
    }

    // ── Entity factory helpers ──────────────────────────────────────

    private ChiTieuBscChiNhanh newPhong(ChiTieuBscRequest req, int nam) {
        ChiTieuBscChiNhanh e = initBase(nam);
        e.setMaDonViCap6(req.getMaUnit());
        e.setTenDonViCap6(req.getTenUnit());
        return e;
    }

    private ChiTieuBscChiNhanh newCn(ChiTieuBscRequest req, int nam) {
        ChiTieuBscChiNhanh e = initBase(nam);
        e.setMaCn(req.getMaUnit());
        e.setTenCn(req.getTenUnit());
        return e;
    }

    private ChiTieuBscChiNhanh newAm(ChiTieuBscRequest req, int nam) {
        ChiTieuBscChiNhanh e = initBase(nam);
        e.setMaAm(req.getMaUnit());
        e.setTenAm(req.getTenUnit());
        return e;
    }

    private ChiTieuBscChiNhanh initBase(int nam) {
        ChiTieuBscChiNhanh e = new ChiTieuBscChiNhanh();
        e.setTypeData(0);
        e.setNam(nam);
        e.setHuyDongVonCuoiKy(BigDecimal.ZERO);
        e.setHuyDongVonBinhQuan(BigDecimal.ZERO);
        e.setCasaBinhQuan(BigDecimal.ZERO);
        e.setDuNoTinDungCuoiKy(BigDecimal.ZERO);
        e.setThuNhapThuanDichVu(BigDecimal.ZERO);
        e.setThuNhapThuanHdvFtp(BigDecimal.ZERO);
        e.setThuNhapThuanTinDung(BigDecimal.ZERO);
        e.setThuNhapThuan(BigDecimal.ZERO);
        return e;
    }

    // ── Data loaders ──────────────────────────────────────────────────

    private List<Object[]> loadKeHoach(String doiTuong, int nam) {
        return switch (norm(doiTuong)) {
            case "chi-nhanh" -> repo.keHoachCnByNam(nam);
            case "am"        -> repo.keHoachAmByNam(nam);
            default          -> repo.keHoachPhongByNam(nam);
        };
    }

    private List<Object[]> loadThucHien(String loaiKy, String selectedKy, String doiTuong) {
        return switch (norm(loaiKy)) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                String quy = "Q" + p[0].replace("Q", "");
                int nam = Integer.parseInt(p[1]);
                yield switch (norm(doiTuong)) {
                    case "chi-nhanh" -> repo.thucHienCnByQuyNam(quy, nam);
                    case "am"        -> repo.thucHienAmByQuyNam(quy, nam);
                    default          -> repo.thucHienPhongByQuyNam(quy, nam);
                };
            }
            case "nam" -> {
                int nam = Integer.parseInt(selectedKy);
                yield switch (norm(doiTuong)) {
                    case "chi-nhanh" -> repo.thucHienCnByNam(nam);
                    case "am"        -> repo.thucHienAmByNam(nam);
                    default          -> repo.thucHienPhongByNam(nam);
                };
            }
            default -> {  // thang
                String[] p = selectedKy.split("/");
                int thang = Integer.parseInt(p[0]);
                int nam   = Integer.parseInt(p[1]);
                yield switch (norm(doiTuong)) {
                    case "chi-nhanh" -> repo.thucHienCnByThangNam(thang, nam);
                    case "am"        -> repo.thucHienAmByThangNam(thang, nam);
                    default          -> repo.thucHienPhongByThangNam(thang, nam);
                };
            }
        };
    }

    // ── Period helpers ─────────────────────────────────────────────

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

    private int parseNam(String loaiKy, String selectedKy) {
        if ("nam".equals(norm(loaiKy))) return Integer.parseInt(selectedKy);
        String[] p = selectedKy.split("/");
        return Integer.parseInt(p[p.length - 1]);
    }

    // ── Object[] helpers ──────────────────────────────────────────

    private Map<String, Object[]> toMap(List<Object[]> rows) {
        return rows.stream()
            .filter(r -> r[0] != null)
            .collect(Collectors.toMap(r -> str(r[0]), r -> r, (a, b) -> a, LinkedHashMap::new));
    }

    private BigDecimal[] extractVals(Object[] r) {
        if (r == null) return new BigDecimal[]{BD0, BD0, BD0, BD0, BD0, BD0, BD0};
        return new BigDecimal[]{ bd(r[2]), bd(r[3]), bd(r[4]), bd(r[5]), bd(r[6]), bd(r[7]), bd(r[8]) };
    }

    private static final BigDecimal BD0 = BigDecimal.ZERO;

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
