package com.mpa.service.impl;

import com.mpa.dto.FileImportResult;
import com.mpa.entity.PhongBan;
import com.mpa.repository.PhongBanRepository;
import com.mpa.service.ThongTinAmImportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Import file "Thông tin AM" (mã định danh) — nhận diện qua ô tiêu đề đầu tiên = "Mã định danh"
 * (không dựa vào tên file, khác với ISS_02/MPA). Ghi thẳng (upsert theo ma_am) vào thong_tin_am,
 * không qua staging vì đây là dữ liệu master nhỏ, có khóa nghiệp vụ ổn định (ma_am).
 */
@Service
@RequiredArgsConstructor
public class ThongTinAmImportServiceImpl implements ThongTinAmImportService {

    private static final String LOAI_FILE = "THONG_TIN_AM";

    private static final String HEADER_MA_DINH_DANH = "Mã định danh";
    private static final String HEADER_CHI_NHANH = "Chi nhánh";
    private static final String HEADER_PHONG = "Phòng";
    private static final String HEADER_CAP = "Cấp";
    private static final String HEADER_USER_LIEN_KET = "User liên kết";
    private static final String HEADER_TRANG_THAI = "Trạng thái mã định danh";

    private static final List<String> REQUIRED_HEADERS = List.of(
            HEADER_MA_DINH_DANH, HEADER_CHI_NHANH, HEADER_PHONG, HEADER_CAP, HEADER_USER_LIEN_KET, HEADER_TRANG_THAI);

    private final PhongBanRepository phongBanRepo;
    private final JdbcTemplate jdbcTemplate;

    private record ParsedRow(String maAm, String maCn, String maDonViCap6, String tenAm,
                              String chucVu, String maCanBo, Short trangThai) {}

    @Override
    public boolean isThongTinAmFile(byte[] excelBytes) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) return false;
            String first = getCellAsString(header.getCell(0));
            return HEADER_MA_DINH_DANH.equals(first == null ? null : first.trim());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public FileImportResult importFile(InputStream excelStream, String fileName) {
        try (Workbook wb = WorkbookFactory.create(excelStream)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return new FileImportResult(fileName, LOAI_FILE, 0, "FAILED", "File rỗng, không có dòng tiêu đề", null);
            }

            Map<String, Integer> headerIndex = new HashMap<>();
            for (Cell c : headerRow) {
                String h = getCellAsString(c);
                if (h != null) headerIndex.put(h.trim(), c.getColumnIndex());
            }

            List<String> missing = REQUIRED_HEADERS.stream()
                    .filter(h -> !headerIndex.containsKey(h))
                    .collect(Collectors.toList());
            if (!missing.isEmpty()) {
                return new FileImportResult(fileName, LOAI_FILE, 0, "FAILED",
                        "Thiếu cột: " + String.join(", ", missing), null);
            }

            // Map tên phòng đã chuẩn hoá (upper, "P " -> "PHONG ") -> ma_don_vi_cap_6, để tra cứu.
            Map<String, String> maDonViByTenPhong = phongBanRepo.findAll().stream()
                    .filter(p -> p.getTenDonViCap6() != null && p.getMaDonViCap6() != null)
                    .collect(Collectors.toMap(
                            p -> p.getTenDonViCap6().trim().toUpperCase(),
                            PhongBan::getMaDonViCap6,
                            (a, b) -> a));

            // Trùng ma_am trong cùng 1 file -> giữ dòng cuối cùng (LinkedHashMap, duyệt theo thứ tự file).
            LinkedHashMap<String, ParsedRow> byMaAm = new LinkedHashMap<>();
            int skipped = 0;
            int khongKhopPhong = 0;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String maAm = textNumSafeOrNull(getCellAsString(row.getCell(headerIndex.get(HEADER_MA_DINH_DANH))));
                if (maAm == null) { skipped++; continue; }

                String maCn = beforeFirstDash(getCellAsString(row.getCell(headerIndex.get(HEADER_CHI_NHANH))));

                String tenPhongChuan = chuanHoaTenPhong(getCellAsString(row.getCell(headerIndex.get(HEADER_PHONG))));
                String maDonViCap6 = tenPhongChuan != null ? maDonViByTenPhong.get(tenPhongChuan) : null;
                if (tenPhongChuan != null && maDonViCap6 == null) khongKhopPhong++;

                String capRaw = getCellAsString(row.getCell(headerIndex.get(HEADER_CAP)));
                String chucVu = capRaw != null ? capRaw.trim() : null;

                String[] userParts = splitLastDash(getCellAsString(row.getCell(headerIndex.get(HEADER_USER_LIEN_KET))));
                String tenAm = userParts[0];
                String maCanBo = userParts[1];

                Short trangThai = parseLeadingShort(getCellAsString(row.getCell(headerIndex.get(HEADER_TRANG_THAI))));

                byMaAm.put(maAm, new ParsedRow(maAm, maCn, maDonViCap6, tenAm, chucVu, maCanBo, trangThai));
            }

            List<ParsedRow> rows = new ArrayList<>(byMaAm.values());
            if (rows.isEmpty()) {
                return new FileImportResult(fileName, LOAI_FILE, 0, "FAILED", "Không có dòng dữ liệu hợp lệ", null);
            }

            // Dữ liệu cũ thong_tin_am hiện có nhiều dòng trùng ma_am (nhập tay trước đây, chưa có
            // ràng buộc unique) — không tự ý xoá/gộp; chỉ cập nhật dòng có id lớn nhất (mới nhất)
            // cho mỗi ma_am, dòng nào chưa từng có thì thêm mới.
            List<Map<String, Object>> existingRows = jdbcTemplate.queryForList(
                    "SELECT DISTINCT ON (ma_am) ma_am, id FROM thong_tin_am WHERE ma_am IS NOT NULL ORDER BY ma_am, id DESC");
            Map<String, Integer> existingId = new HashMap<>();
            for (Map<String, Object> row : existingRows) {
                existingId.put((String) row.get("ma_am"), ((Number) row.get("id")).intValue());
            }

            List<ParsedRow> toInsert = new ArrayList<>();
            List<ParsedRow> toUpdate = new ArrayList<>();
            for (ParsedRow p : rows) {
                if (existingId.containsKey(p.maAm())) toUpdate.add(p); else toInsert.add(p);
            }

            if (!toInsert.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO thong_tin_am (ma_cn, ma_don_vi_cap_6, ma_am, ten_am, chuc_vu, trang_thai, ma_can_bo) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        toInsert, toInsert.size(), (ps, p) -> {
                            ps.setString(1, p.maCn());
                            ps.setString(2, p.maDonViCap6());
                            ps.setString(3, p.maAm());
                            ps.setString(4, p.tenAm());
                            ps.setString(5, p.chucVu());
                            if (p.trangThai() != null) ps.setShort(6, p.trangThai()); else ps.setNull(6, Types.SMALLINT);
                            ps.setString(7, p.maCanBo());
                        });
            }
            if (!toUpdate.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "UPDATE thong_tin_am SET ma_cn=?, ma_don_vi_cap_6=?, ten_am=?, chuc_vu=?, trang_thai=?, ma_can_bo=? WHERE id=?",
                        toUpdate, toUpdate.size(), (ps, p) -> {
                            ps.setString(1, p.maCn());
                            ps.setString(2, p.maDonViCap6());
                            ps.setString(3, p.tenAm());
                            ps.setString(4, p.chucVu());
                            if (p.trangThai() != null) ps.setShort(5, p.trangThai()); else ps.setNull(5, Types.SMALLINT);
                            ps.setString(6, p.maCanBo());
                            ps.setInt(7, existingId.get(p.maAm()));
                        });
            }

            StringBuilder ghiChu = new StringBuilder();
            ghiChu.append(toInsert.size()).append(" thêm mới, ").append(toUpdate.size()).append(" cập nhật");
            if (skipped > 0) ghiChu.append(", ").append(skipped).append(" dòng bỏ qua (thiếu mã định danh)");
            if (khongKhopPhong > 0) ghiChu.append(", ").append(khongKhopPhong).append(" dòng không khớp phòng ban");

            return new FileImportResult(fileName, LOAI_FILE, rows.size(), "SUCCESS", ghiChu.toString(), null);
        } catch (Exception e) {
            return new FileImportResult(fileName, LOAI_FILE, 0, "FAILED", "Lỗi xử lý file: " + e.getMessage(), null);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static String beforeFirstDash(String s) {
        if (s == null) return null;
        int i = s.indexOf('-');
        String v = (i >= 0 ? s.substring(0, i) : s).trim();
        return v.isEmpty() ? null : v;
    }

    /** "21-P Khach hang doanh nghiep 1" -> "PHONG KHACH HANG DOANH NGHIEP 1" (không đụng "PGD ..."). */
    private static String chuanHoaTenPhong(String phongRaw) {
        if (phongRaw == null) return null;
        int i = phongRaw.indexOf('-');
        String ten = (i >= 0 ? phongRaw.substring(i + 1) : phongRaw).trim().toUpperCase();
        ten = ten.replaceFirst("^P\\s+", "PHONG ");
        return ten.isEmpty() ? null : ten;
    }

    /** "Nguyễn Xuân Quang-158645" -> ["Nguyễn Xuân Quang", "158645"]. */
    private static String[] splitLastDash(String s) {
        if (s == null) return new String[]{null, null};
        int i = s.lastIndexOf('-');
        if (i < 0) return new String[]{s.trim(), null};
        String ten = s.substring(0, i).trim();
        String ma = s.substring(i + 1).trim();
        return new String[]{ten.isEmpty() ? null : ten, ma.isEmpty() ? null : ma};
    }

    /** "1-Active" -> 1, "2-Closed" -> 2. */
    private static Short parseLeadingShort(String s) {
        if (s == null) return null;
        int i = s.indexOf('-');
        String num = (i >= 0 ? s.substring(0, i) : s).trim();
        try { return Short.parseShort(num); } catch (Exception e) { return null; }
    }

    private static String textNumSafeOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        String cleaned = t.replace(",", "");
        if (cleaned.matches("-?\\d+\\.0+")) cleaned = cleaned.substring(0, cleaned.indexOf('.'));
        return cleaned;
    }

    private static String getCellAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                String s = cell.getStringCellValue();
                return (s == null || s.isBlank()) ? null : s.trim();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); }
                catch (Exception e) {
                    try { return String.valueOf(cell.getNumericCellValue()); }
                    catch (Exception e2) { return null; }
                }
            default:
                return null;
        }
    }
}
