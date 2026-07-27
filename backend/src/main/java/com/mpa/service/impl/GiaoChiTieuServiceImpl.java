package com.mpa.service.impl;

import com.mpa.dto.BscSoSanhResponse;
import com.mpa.dto.BscSoSanhRowResponse;
import com.mpa.dto.ChiTieuBscRequest;
import com.mpa.dto.ChiTieuQuanLyRow;
import com.mpa.dto.XuHuongMetricSeries;
import com.mpa.dto.XuHuongResponse;
import com.mpa.entity.ChiTieuBscChiNhanh;
import com.mpa.entity.ThongTinAm;
import com.mpa.repository.ChiNhanhRepository;
import com.mpa.repository.ChiTieuBscRepository;
import com.mpa.repository.PhongBanRepository;
import com.mpa.repository.ThongTinAmRepository;
import com.mpa.repository.ThucHienBscRepository;
import com.mpa.service.GiaoChiTieuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GiaoChiTieuServiceImpl implements GiaoChiTieuService {

    private final ChiTieuBscRepository repo;
    private final ThucHienBscRepository thucHienRepo;
    private final ThongTinAmRepository thongTinAmRepo;
    private final PhongBanRepository phongBanRepo;
    private final ChiNhanhRepository chiNhanhRepo;

    @Override
    public BscSoSanhResponse getSoSanh(String loaiKy, String selectedKy, String doiTuong, String maDonViCap6) {
        List<Object[]> khRows   = loadKeHoach(doiTuong, loaiKy, selectedKy, maDonViCap6);
        List<Object[]> thRows   = loadThucHien(loaiKy, selectedKy, doiTuong, maDonViCap6);
        // "Ngày" luôn so với dữ liệu mới nhất — không có khái niệm "kỳ trước" để tính Δ%.
        List<Object[]> prevRows = "ngay".equals(norm(loaiKy))
            ? List.of()
            : loadThucHien(loaiKy, calcPrevKy(loaiKy, selectedKy), doiTuong, maDonViCap6);

        Map<String, Object[]> khMap   = toMap(khRows);
        Map<String, Object[]> thMap   = toMap(thRows);
        Map<String, Object[]> prevMap = toMap(prevRows);

        // Chỉ lấy key từ các dòng có đơn vị thật (r[0] != null) — khớp với toMap() ở trên,
        // tránh sinh dòng "ma" khi 1 dòng lọt qua group-by do thiếu điều kiện IS NOT NULL.
        Set<String> allKeys = new LinkedHashSet<>();
        thRows.stream().filter(r -> r[0] != null).map(r -> str(r[0])).forEach(allKeys::add);
        khRows.stream().filter(r -> r[0] != null).map(r -> str(r[0])).forEach(allKeys::add);

        // AM: liệt kê đủ toàn bộ cán bộ AM thuộc phòng (hoặc toàn bộ nếu không lọc phòng) theo
        // danh sách gốc thong_tin_am — kể cả những AM chưa có thực hiện/kế hoạch cho kỳ này
        // (hiển thị "Chưa giao chỉ tiêu" thay vì bị ẩn hoàn toàn khỏi bảng).
        Map<String, String> amRosterNames = new LinkedHashMap<>();
        if ("am".equals(norm(doiTuong))) {
            String don = (maDonViCap6 == null || maDonViCap6.isBlank()) ? null : maDonViCap6;
            for (ThongTinAm t : thongTinAmRepo.searchList("", null, don)) {
                if (t.getMaAm() == null) continue;
                allKeys.add(t.getMaAm());
                amRosterNames.putIfAbsent(t.getMaAm(), t.getTenAm() != null ? t.getTenAm() : t.getMaAm());
            }
        }

        int dat = 0, canh = 0, rui = 0, chuaGiao = 0;
        List<BscSoSanhRowResponse> rows = new ArrayList<>();

        for (String key : allKeys) {
            BigDecimal[] thV   = extractVals(thMap.get(key));
            BigDecimal[] khV   = extractVals(khMap.get(key));
            BigDecimal[] prevV = extractVals(prevMap.get(key));

            String name = thMap.containsKey(key) ? str(thMap.get(key)[1])
                        : khMap.containsKey(key) ? str(khMap.get(key)[1])
                        : amRosterNames.getOrDefault(key, key);

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

        // AM: sắp xếp theo tên (kiểu danh sách cán bộ) thay vì theo giá trị TNT.
        if ("am".equals(norm(doiTuong))) {
            rows.sort(Comparator.comparing(BscSoSanhRowResponse::getTenUnit, String.CASE_INSENSITIVE_ORDER));
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
        Ky ky = resolveKy(req.getLoaiKy(), req.getSelectedKy());
        ChiTieuBscChiNhanh entity = switch (norm(req.getDoiTuong())) {
            case "chi-nhanh" -> findKeHoachCn(ky, req.getMaUnit())
                .orElseGet(() -> newCn(req, ky));
            case "am" -> findKeHoachAm(ky, req.getMaUnit())
                .orElseGet(() -> newAm(req, ky));
            default -> findKeHoachPhong(ky, req.getMaUnit())
                .orElseGet(() -> newPhong(req, ky));
        };
        applyKpi(entity, req.getChiTieu(), req.getMucTieu());
        entity.setNgayTao(LocalDateTime.now());
        repo.save(entity);
    }

    private Optional<ChiTieuBscChiNhanh> findKeHoachPhong(Ky ky, String ma) {
        return switch (ky.typeData()) {
            case 1  -> repo.findKeHoachPhongThangNam(ky.thang(), ky.nam(), ma);
            case 5  -> repo.findKeHoachPhongQuyNam(ky.quy(), ky.nam(), ma);
            default -> repo.findKeHoachPhongNam(ky.nam(), ma);
        };
    }

    private Optional<ChiTieuBscChiNhanh> findKeHoachCn(Ky ky, String ma) {
        return switch (ky.typeData()) {
            case 1  -> repo.findKeHoachCnThangNam(ky.thang(), ky.nam(), ma);
            case 5  -> repo.findKeHoachCnQuyNam(ky.quy(), ky.nam(), ma);
            default -> repo.findKeHoachCnNam(ky.nam(), ma);
        };
    }

    private Optional<ChiTieuBscChiNhanh> findKeHoachAm(Ky ky, String ma) {
        return switch (ky.typeData()) {
            case 1  -> repo.findKeHoachAmThangNam(ky.thang(), ky.nam(), ma);
            case 5  -> repo.findKeHoachAmQuyNam(ky.quy(), ky.nam(), ma);
            default -> repo.findKeHoachAmNam(ky.nam(), ma);
        };
    }

    @Override
    public void deleteChiTieu(Integer id) {
        repo.deleteById(id);
    }

    // ── QUẢN LÝ LIST ────────────────────────────────────────────────

    @Override
    public List<ChiTieuQuanLyRow> getQuanLyList(String loaiKy, String selectedKy, String doiTuong, String maDonViCap6) {
        Ky ky = resolveKy(loaiKy, selectedKy);
        List<ChiTieuBscChiNhanh> list = switch (norm(doiTuong)) {
            case "chi-nhanh" -> findKeHoachCnList(ky);
            case "am"        -> findKeHoachAmList(ky);
            default          -> findKeHoachPhongList(ky);
        };
        return mapQuanLyRows(filterEntitiesByPhong(list, doiTuong, maDonViCap6), norm(doiTuong));
    }

    // Chi_tieu_bsc_chi_nhanh không lưu phòng ban cho dòng kế hoạch AM — lọc theo tập mã AM
    // thuộc phòng (tra từ thong_tin_am, bảng danh sách cán bộ AM). Đối tượng "chi-nhanh"
    // không áp dụng (chỉ 1 chi nhánh).
    private List<ChiTieuBscChiNhanh> filterEntitiesByPhong(List<ChiTieuBscChiNhanh> list, String doiTuong, String maDonViCap6) {
        if (maDonViCap6 == null || maDonViCap6.isBlank()) return list;
        if ("phong".equals(norm(doiTuong))) {
            return list.stream().filter(c -> maDonViCap6.equals(c.getMaDonViCap6())).collect(Collectors.toList());
        }
        if ("am".equals(norm(doiTuong))) {
            Set<String> amCodes = amCodesByPhong(maDonViCap6);
            return list.stream().filter(c -> amCodes.contains(c.getMaAm())).collect(Collectors.toList());
        }
        return list;
    }

    private Set<String> amCodesByPhong(String maDonViCap6) {
        return thongTinAmRepo.searchList("", null, maDonViCap6).stream()
            .map(ThongTinAm::getMaAm)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private List<ChiTieuBscChiNhanh> findKeHoachPhongList(Ky ky) {
        return switch (ky.typeData()) {
            case 1  -> repo.findKeHoachPhongListThangNam(ky.thang(), ky.nam());
            case 5  -> repo.findKeHoachPhongListQuyNam(ky.quy(), ky.nam());
            default -> repo.findKeHoachPhongListNam(ky.nam());
        };
    }

    private List<ChiTieuBscChiNhanh> findKeHoachCnList(Ky ky) {
        return switch (ky.typeData()) {
            case 1  -> repo.findKeHoachCnListThangNam(ky.thang(), ky.nam());
            case 5  -> repo.findKeHoachCnListQuyNam(ky.quy(), ky.nam());
            default -> repo.findKeHoachCnListNam(ky.nam());
        };
    }

    private List<ChiTieuBscChiNhanh> findKeHoachAmList(Ky ky) {
        return switch (ky.typeData()) {
            case 1  -> repo.findKeHoachAmListThangNam(ky.thang(), ky.nam());
            case 5  -> repo.findKeHoachAmListQuyNam(ky.quy(), ky.nam());
            default -> repo.findKeHoachAmListNam(ky.nam());
        };
    }

    private List<ChiTieuQuanLyRow> mapQuanLyRows(List<ChiTieuBscChiNhanh> list, String kind) {
        return list.stream().map(c -> {
            String maUnit  = switch (kind) {
                case "chi-nhanh" -> c.getMaCn();
                case "am"        -> c.getMaAm();
                default          -> c.getMaDonViCap6();
            };
            String tenUnit = switch (kind) {
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
    public List<Map<String, String>> getAmList(String maDonViCap6) {
        String don = (maDonViCap6 == null || maDonViCap6.isBlank()) ? null : maDonViCap6;
        return thongTinAmRepo.searchList("", null, don).stream()
            .filter(t -> t.getMaAm() != null)
            .map(t -> Map.of("ma", t.getMaAm(), "ten", t.getTenAm() != null ? t.getTenAm() : t.getMaAm()))
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

    private ChiTieuBscChiNhanh newPhong(ChiTieuBscRequest req, Ky ky) {
        ChiTieuBscChiNhanh e = initBase(ky);
        e.setMaDonViCap6(req.getMaUnit());
        e.setTenDonViCap6(req.getTenUnit());
        return e;
    }

    private ChiTieuBscChiNhanh newCn(ChiTieuBscRequest req, Ky ky) {
        ChiTieuBscChiNhanh e = initBase(ky);
        e.setMaCn(req.getMaUnit());
        e.setTenCn(req.getTenUnit());
        return e;
    }

    private ChiTieuBscChiNhanh newAm(ChiTieuBscRequest req, Ky ky) {
        ChiTieuBscChiNhanh e = initBase(ky);
        e.setMaAm(req.getMaUnit());
        e.setTenAm(req.getTenUnit());
        return e;
    }

    private ChiTieuBscChiNhanh initBase(Ky ky) {
        ChiTieuBscChiNhanh e = new ChiTieuBscChiNhanh();
        e.setTypeData(ky.typeData());
        e.setThang(ky.thang());
        e.setQuy(ky.quy());
        e.setNam(ky.nam());
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

    private List<Object[]> loadKeHoach(String doiTuong, String loaiKy, String selectedKy, String maDonViCap6) {
        List<Object[]> rows = switch (norm(loaiKy)) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                String quy = "Q" + p[0].replace("Q", "");
                int nam = Integer.parseInt(p[1]);
                yield switch (norm(doiTuong)) {
                    case "chi-nhanh" -> repo.keHoachCnByQuyNam(quy, nam);
                    case "am"        -> repo.keHoachAmByQuyNam(quy, nam);
                    default          -> repo.keHoachPhongByQuyNam(quy, nam);
                };
            }
            case "nam", "ngay" -> {
                // "Ngày" so với chỉ tiêu BSC theo năm đã khai báo — dùng chung kế hoạch năm.
                int nam = Integer.parseInt(selectedKy);
                yield switch (norm(doiTuong)) {
                    case "chi-nhanh" -> repo.keHoachCnByNam(nam);
                    case "am"        -> repo.keHoachAmByNam(nam);
                    default          -> repo.keHoachPhongByNam(nam);
                };
            }
            default -> {  // thang
                String[] p = selectedKy.split("/");
                int thang = Integer.parseInt(p[0]);
                int nam   = Integer.parseInt(p[1]);
                yield switch (norm(doiTuong)) {
                    case "chi-nhanh" -> repo.keHoachCnByThangNam(thang, nam);
                    case "am"        -> repo.keHoachAmByThangNam(thang, nam);
                    default          -> repo.keHoachPhongByThangNam(thang, nam);
                };
            }
        };
        return filterRowsByPhong(rows, doiTuong, maDonViCap6);
    }

    // Đọc "thực hiện" từ thuc_hien_bsc_chi_nhanh (ThucHienBscRepository) — bảng thực tế được
    // BscSyncServiceImpl đồng bộ từ du_lieu_mpa. chi_tieu_bsc_chi_nhanh (repo ở trên) chỉ chứa
    // kế hoạch (type_data=0/1/5), KHÔNG bao giờ có dữ liệu thực hiện.
    private List<Object[]> loadThucHien(String loaiKy, String selectedKy, String doiTuong, String maDonViCap6) {
        List<Object[]> rows = switch (norm(loaiKy)) {
            case "ngay" -> switch (norm(doiTuong)) {
                case "chi-nhanh" -> thucHienRepo.cnRowByNgayLatest();
                case "am"        -> thucHienRepo.amRowByNgayLatest();
                default          -> thucHienRepo.phongTableByNgayLatest();
            };
            case "quy" -> {
                String[] p = selectedKy.split("/");
                String quy = "Q" + p[0].replace("Q", "");
                int nam = Integer.parseInt(p[1]);
                yield switch (norm(doiTuong)) {
                    case "chi-nhanh" -> thucHienRepo.cnRowByQuyNam(quy, nam);
                    case "am"        -> thucHienRepo.amRowByQuyNam(quy, nam);
                    default          -> thucHienRepo.phongTableByQuyNam(quy, nam);
                };
            }
            case "nam" -> {
                int nam = Integer.parseInt(selectedKy);
                yield switch (norm(doiTuong)) {
                    case "chi-nhanh" -> thucHienRepo.cnRowByNam(nam);
                    case "am"        -> thucHienRepo.amRowByNam(nam);
                    default          -> thucHienRepo.phongTableByNam(nam);
                };
            }
            default -> {  // thang
                String[] p = selectedKy.split("/");
                int thang = Integer.parseInt(p[0]);
                int nam   = Integer.parseInt(p[1]);
                yield switch (norm(doiTuong)) {
                    case "chi-nhanh" -> thucHienRepo.cnRowByThangNam(thang, nam);
                    case "am"        -> thucHienRepo.amRowByThangNam(thang, nam);
                    default          -> thucHienRepo.phongTableByThangNam(thang, nam);
                };
            }
        };
        return filterRowsByPhong(rows, doiTuong, maDonViCap6);
    }

    // "Chi nhánh" không áp dụng (chỉ 1 chi nhánh). "Phòng": lọc thẳng theo mã đơn vị (Object[0]).
    // "AM": chi_tieu_bsc_chi_nhanh/thuc_hien_bsc_chi_nhanh không có cột phòng ban đáng tin cậy
    // dùng chung cho mọi kỳ — lọc theo tập mã AM thuộc phòng, tra từ thong_tin_am.
    private List<Object[]> filterRowsByPhong(List<Object[]> rows, String doiTuong, String maDonViCap6) {
        if (maDonViCap6 == null || maDonViCap6.isBlank()) return rows;
        if ("phong".equals(norm(doiTuong))) {
            return rows.stream().filter(r -> maDonViCap6.equals(str(r[0]))).collect(Collectors.toList());
        }
        if ("am".equals(norm(doiTuong))) {
            Set<String> amCodes = amCodesByPhong(maDonViCap6);
            return rows.stream().filter(r -> amCodes.contains(str(r[0]))).collect(Collectors.toList());
        }
        return rows;
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

    // typeData: kế hoạch năm = 0, kế hoạch tháng = 1, kế hoạch quý = 5 — khớp với type_data
    // của "thực hiện" cho THÁNG/QUÝ, riêng NĂM giữ 0 (quy ước cũ, dữ liệu kế hoạch năm đã có sẵn).
    private record Ky(int typeData, Integer thang, String quy, int nam) {}

    private Ky resolveKy(String loaiKyRaw, String selectedKy) {
        return switch (norm(loaiKyRaw)) {
            case "quy" -> {
                String[] p = selectedKy.split("/");
                String quy = "Q" + p[0].replace("Q", "");
                int nam = Integer.parseInt(p[1]);
                yield new Ky(5, null, quy, nam);
            }
            // "Ngày" không có kế hoạch riêng — Thêm/Sửa chỉ tiêu khi đang xem "Ngày" sẽ ghi
            // vào đúng kế hoạch năm (đã khai báo), khớp với cách so sánh ở loadKeHoach/getSoSanh.
            case "nam", "ngay" -> new Ky(0, null, null, Integer.parseInt(selectedKy));
            default -> {  // thang
                String[] p = selectedKy.split("/");
                int thang = Integer.parseInt(p[0]);
                int nam   = Integer.parseInt(p[1]);
                yield new Ky(1, thang, null, nam);
            }
        };
    }

    @Override
    public String getLatestNgay() {
        var d = thucHienRepo.findLatestNgay();
        return d == null ? null : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ── XU HƯỚNG (7 biểu đồ: kế hoạch cột + thực hiện đường) ────────

    // Đúng thứ tự + nhãn 7 cột của bảng "Dashboard so sánh" — cột i+1 trong Object[] kết quả
    // trend (period ở cột 0) khớp thẳng với index i ở đây.
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
    public XuHuongResponse getXuHuong(String loaiKy, int nam, String doiTuong, String maDonViCap6, String maAm) {
        String lk = norm(loaiKy);
        if ("ngay".equals(lk)) {
            return XuHuongResponse.builder().periods(List.of()).metrics(List.of()).build();
        }

        // Thứ tự ưu tiên: đã thu hẹp về 1 mã AM cụ thể > đã chọn 1 phòng ban > toàn chi nhánh.
        // Áp dụng chung cho cả Đối tượng giao Phòng và AM — không cần nhánh riêng theo doiTuong.
        boolean amScoped    = maAm != null && !maAm.isBlank();
        boolean phongScoped = !amScoped && maDonViCap6 != null && !maDonViCap6.isBlank();

        List<String> periods;
        List<Object[]> khRows;
        List<Object[]> thRows;

        switch (lk) {
            case "quy" -> {
                periods = List.of("Q1", "Q2", "Q3", "Q4");
                khRows = amScoped ? repo.keHoachTrendAmByQuy(maAm, nam)
                       : phongScoped ? repo.keHoachTrendPhongByQuy(maDonViCap6, nam)
                       : repo.keHoachTrendCnByQuy(nam);
                thRows = amScoped ? thucHienRepo.trendAmOneByQuy(maAm, nam)
                       : phongScoped ? thucHienRepo.trendPhongOneByQuy(maDonViCap6, nam)
                       : thucHienRepo.trendAllByQuy(nam);
            }
            case "nam" -> {
                List<Integer> years = List.of(nam - 3, nam - 2, nam - 1, nam);
                periods = years.stream().map(String::valueOf).collect(Collectors.toList());
                khRows = amScoped ? repo.keHoachTrendAmByNam(maAm, years)
                       : phongScoped ? repo.keHoachTrendPhongByNam(maDonViCap6, years)
                       : repo.keHoachTrendCnByNam(years);
                thRows = amScoped ? thucHienRepo.trendAmOneByNam(maAm, years)
                       : phongScoped ? thucHienRepo.trendPhongOneByNam(maDonViCap6, years)
                       : thucHienRepo.trendAllByNam(years);
            }
            default -> {  // thang
                periods = IntStream.rangeClosed(1, 12)
                    .mapToObj(m -> String.format("T%02d", m)).collect(Collectors.toList());
                khRows = amScoped ? repo.keHoachTrendAmByThang(maAm, nam)
                       : phongScoped ? repo.keHoachTrendPhongByThang(maDonViCap6, nam)
                       : repo.keHoachTrendCnByThang(nam);
                thRows = amScoped ? thucHienRepo.trendAmOneByThang(maAm, nam)
                       : phongScoped ? thucHienRepo.trendPhongOneByThang(maDonViCap6, nam)
                       : thucHienRepo.trendAllByThang(nam);
            }
        }

        Map<String, Object[]> khMap = keyByPeriod(khRows, lk);
        Map<String, Object[]> thMap = keyByPeriod(thRows, lk);

        List<XuHuongMetricSeries> metrics = new ArrayList<>();
        for (int i = 0; i < METRIC_DEFS.length; i++) {
            int col = i + 1; // Object[0]=period, [1..7]=7 chỉ tiêu đúng thứ tự METRIC_DEFS
            List<BigDecimal> kh = new ArrayList<>();
            List<BigDecimal> th = new ArrayList<>();
            for (String p : periods) {
                kh.add(valAt(khMap.get(p), col));
                th.add(valAt(thMap.get(p), col));
            }
            metrics.add(XuHuongMetricSeries.builder()
                .metricKey(METRIC_DEFS[i][0]).metricLabel(METRIC_DEFS[i][1])
                .keHoachValues(kh).thucHienValues(th)
                .build());
        }

        return XuHuongResponse.builder().periods(periods).metrics(metrics).build();
    }

    // Chuẩn hoá period của từng dòng trend về đúng dạng chuỗi trong `periods` ("T01".."T12" /
    // "Q1".."Q4" / năm) để map theo key thay vì theo thứ tự (phòng trường hợp thiếu period nào đó).
    private Map<String, Object[]> keyByPeriod(List<Object[]> rows, String loaiKy) {
        return rows.stream().filter(r -> r[0] != null).collect(Collectors.toMap(
            r -> "thang".equals(norm(loaiKy)) ? String.format("T%02d", ((Number) r[0]).intValue()) : str(r[0]),
            r -> r, (a, b) -> a, LinkedHashMap::new));
    }

    private BigDecimal valAt(Object[] r, int col) {
        return r == null ? BD0 : bd(r[col]);
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
