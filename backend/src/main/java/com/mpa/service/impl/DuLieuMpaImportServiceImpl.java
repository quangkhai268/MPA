package com.mpa.service.impl;

import com.mpa.dto.DuLieuMpaPeriod;
import com.mpa.dto.FileImportResult;
import com.mpa.service.DuLieuMpaImportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Đọc file Excel MPA (4 loại kỳ: Tháng/Quý/Năm/Ngày, tự nhận diện qua header dòng đầu
 * tiên — không dựa vào tên file) bằng SAX streaming và nạp vào bảng du_lieu_mpa.
 * Mỗi loại kỳ là dữ liệu lũy tiến độc lập (xem CLAUDE.md / plan) nên commit dùng
 * replace-by-period RIÊNG cho từng loại (loai_ky), không đụng tới các loại/kỳ khác.
 */
@Service
@RequiredArgsConstructor
public class DuLieuMpaImportServiceImpl implements DuLieuMpaImportService {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 500;

    private static final Pattern THANG_PATTERN = Pattern.compile("^\\s*(\\d{4})\\s*/\\s*(\\d{1,2})\\s*$");
    private static final Pattern QUY_PATTERN = Pattern.compile("^\\s*(\\d{4})-Q([1-4])\\s*$");
    // File loại Năm là lũy kế đến 1 tháng "đang di chuyển" — tháng đó không có trong nội dung
    // file (chỉ có cột "Năm"), nên phải tách từ TÊN FILE, ví dụ "MPA 2026 (lũy kế đến T5).xlsx".
    private static final Pattern LUY_KE_THANG_PATTERN = Pattern.compile("(?i)T(\\d{1,2})(?!\\d)");

    private enum LoaiKy { THANG, QUY, NAM, NGAY }

    private enum ColType { TEXT, TEXT_NUM_SAFE, DECIMAL }

    private record ColumnDef(String header, ColType type) {}
    private record SplitColumnDef(String header) {}

    // 4 cột gộp "Mã - Tên X" — tách trên dấu " - " đầu tiên thành ma_*/ten_*.
    private static final List<SplitColumnDef> SPLIT_COLUMNS = List.of(
            new SplitColumnDef("Mã - Tên AM"),
            new SplitColumnDef("Mã - Tên đơn vị tổ chức cấp 6"),
            new SplitColumnDef("Mã - Tên SP cấp 5"),
            new SplitColumnDef("Mã - Tên phân khúc KH cấp 2")
    );

    // 10 cột đơn — giống nhau ở cả 4 loại file, thứ tự đúng bằng thứ tự insert vào staging.
    private static final List<ColumnDef> SIMPLE_COLUMNS = List.of(
            new ColumnDef("Mã KH (CIF)", ColType.TEXT_NUM_SAFE),
            new ColumnDef("Tên khách hàng", ColType.TEXT),
            new ColumnDef("Kỳ hạn cấp 2", ColType.TEXT),
            new ColumnDef("Thu nhập thuần từ HĐV (FTP cơ sở + bổ sung)", ColType.DECIMAL),
            new ColumnDef("Thu nhập thuần từ dịch vụ", ColType.DECIMAL),
            new ColumnDef("Thu nhập thuần từ tín dụng (FTP cơ sở +bổ sung)", ColType.DECIMAL),
            new ColumnDef("Thu nhập thuần (FTP cơ sở + bổ sung)", ColType.DECIMAL),
            new ColumnDef("Dư nợ tín dụng cuối kỳ (quy đổi)", ColType.DECIMAL),
            new ColumnDef("Huy động vốn bình quân (quy đổi)", ColType.DECIMAL),
            new ColumnDef("Huy động vốn cuối kỳ (quy đổi)", ColType.DECIMAL)
    );

    private static final String STAGING_INSERT_SQL =
            "INSERT INTO du_lieu_mpa_staging " +
            "(loai_ky, ngay, thang, quy, nam, ma_am, ten_am, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
            " ma_sp_cap_5, ten_sp_cap_5, ma_phan_khuc_kh_cap_2, ten_phan_khuc_kh_cap_2, " +
            " ma_kh_cif, ten_khach_hang, ky_han_cap_2, " +
            " thu_nhap_thuan_hdv_ftp, thu_nhap_thuan_dich_vu, thu_nhap_thuan_tin_dung, thu_nhap_thuan, " +
            " du_no_tin_dung_cuoi_ky, huy_dong_von_binh_quan, huy_dong_von_cuoi_ky, sheetname) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static class HeaderValidationException extends RuntimeException {
        HeaderValidationException(String message) { super(message); }
    }

    @Override
    public void clearStaging() {
        jdbcTemplate.execute("TRUNCATE TABLE du_lieu_mpa_staging");
    }

    @Override
    public long countStaged() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM du_lieu_mpa_staging", Long.class);
        return count != null ? count : 0;
    }

    @Override
    public FileImportResult stageFile(InputStream excelStream, String fileName) {
        RowHandler handler = new RowHandler(fileName);
        try (OPCPackage pkg = OPCPackage.open(excelStream)) {
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();

            while (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    handler.beginSheet(sheets.getSheetName());
                    XMLReader xmlReader = SAXHelper.newXMLReader();
                    xmlReader.setContentHandler(new XSSFSheetXMLHandler(styles, sst, handler, false));
                    xmlReader.parse(new InputSource(sheetStream));
                }
            }
            handler.flush();

            if (handler.totalStaged == 0) {
                return new FileImportResult(fileName, "MPA", 0, "FAILED", "Không có dòng dữ liệu hợp lệ", null);
            }
            List<String> notes = new ArrayList<>();
            if (handler.namLuyKeThang != null) {
                notes.add("Nhận diện: lũy kế đến tháng " + handler.namLuyKeThang);
            }
            if (handler.totalSkipped > 0) {
                notes.add("Bỏ qua " + handler.totalSkipped + " dòng lỗi/trống");
            }
            String ghiChu = notes.isEmpty() ? null : String.join("; ", notes);
            return new FileImportResult(fileName, "MPA", handler.totalStaged, "SUCCESS", ghiChu, handler.buildKyLabel());

        } catch (Exception e) {
            String msg = findHeaderValidationMessage(e);
            if (msg != null) {
                return new FileImportResult(fileName, "MPA", 0, "FAILED", msg, null);
            }
            return new FileImportResult(fileName, "MPA", 0, "FAILED", "Lỗi đọc file: " + e.getMessage(), null);
        }
    }

    private String findHeaderValidationMessage(Throwable t) {
        while (t != null) {
            if (t instanceof HeaderValidationException hve) return hve.getMessage();
            t = t.getCause();
        }
        return null;
    }

    @Override
    @Transactional
    public List<DuLieuMpaPeriod> commitStagedData() {
        jdbcTemplate.execute(
                "DELETE FROM du_lieu_mpa d " +
                "USING (SELECT DISTINCT nam, thang FROM du_lieu_mpa_staging WHERE loai_ky='THANG') s " +
                "WHERE d.loai_ky='THANG' AND d.nam=s.nam AND d.thang=s.thang");

        jdbcTemplate.execute(
                "DELETE FROM du_lieu_mpa d " +
                "USING (SELECT DISTINCT nam, quy FROM du_lieu_mpa_staging WHERE loai_ky='QUY') s " +
                "WHERE d.loai_ky='QUY' AND d.nam=s.nam AND d.quy=s.quy");

        // NAM là lũy kế đến 1 mốc tháng "đang di chuyển" (khác THANG là kỳ đã đóng cố định) —
        // chỉ thay đúng mốc (nam, thang) trùng, GIỮ NGUYÊN các mốc lũy kế khác của cùng năm
        // (đến T3, T4, ... vẫn cần xem lại được sau khi đã có mốc mới hơn).
        jdbcTemplate.execute(
                "DELETE FROM du_lieu_mpa d " +
                "USING (SELECT DISTINCT nam, thang FROM du_lieu_mpa_staging WHERE loai_ky='NAM') s " +
                "WHERE d.loai_ky='NAM' AND d.nam=s.nam AND d.thang=s.thang");

        jdbcTemplate.execute(
                "DELETE FROM du_lieu_mpa d " +
                "USING (SELECT DISTINCT nam FROM du_lieu_mpa_staging WHERE loai_ky='NGAY') s " +
                "WHERE d.loai_ky='NGAY' AND d.nam=s.nam");

        jdbcTemplate.execute(
                "INSERT INTO du_lieu_mpa " +
                "(loai_ky, ngay, thang, quy, nam, ma_am, ten_am, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                " ma_sp_cap_5, ten_sp_cap_5, ma_phan_khuc_kh_cap_2, ten_phan_khuc_kh_cap_2, " +
                " ma_kh_cif, ten_khach_hang, ky_han_cap_2, " +
                " thu_nhap_thuan_hdv_ftp, thu_nhap_thuan_dich_vu, thu_nhap_thuan_tin_dung, thu_nhap_thuan, " +
                " du_no_tin_dung_cuoi_ky, huy_dong_von_binh_quan, huy_dong_von_cuoi_ky, ngay_tao, sheetname) " +
                "SELECT loai_ky, ngay, thang, quy, nam, ma_am, ten_am, ma_don_vi_cap_6, ten_don_vi_cap_6, " +
                "       ma_sp_cap_5, ten_sp_cap_5, ma_phan_khuc_kh_cap_2, ten_phan_khuc_kh_cap_2, " +
                "       ma_kh_cif, ten_khach_hang, ky_han_cap_2, " +
                "       thu_nhap_thuan_hdv_ftp, thu_nhap_thuan_dich_vu, thu_nhap_thuan_tin_dung, thu_nhap_thuan, " +
                "       du_no_tin_dung_cuoi_ky, huy_dong_von_binh_quan, huy_dong_von_cuoi_ky, now(), sheetname " +
                "FROM du_lieu_mpa_staging");

        // Chụp lại đúng (các) kỳ vừa commit TRƯỚC KHI truncate — dùng để đồng bộ tiếp
        // sang thuc_hien_bsc_chi_nhanh/thuc_hien_bsc_khach_hang (BscSyncService) mà
        // không cần quét lại toàn bảng du_lieu_mpa.
        List<DuLieuMpaPeriod> periods = jdbcTemplate.query(
                "SELECT DISTINCT loai_ky, nam, thang, quy, ngay FROM du_lieu_mpa_staging",
                (rs, rowNum) -> new DuLieuMpaPeriod(
                        rs.getString("loai_ky"),
                        (Integer) rs.getObject("nam"),
                        (Integer) rs.getObject("thang"),
                        rs.getString("quy"),
                        rs.getDate("ngay") != null ? rs.getDate("ngay").toLocalDate() : null));

        jdbcTemplate.execute("TRUNCATE TABLE du_lieu_mpa_staging");

        return periods;
    }

    // ── SAX row handler ─────────────────────────────────────────────────

    private class RowHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final Map<Integer, String> currentRow = new HashMap<>();
        private final List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        private final String fileName;

        private String sheetName;
        private Map<String, Integer> headerIdx;
        private LoaiKy loaiKy;
        private LocalDate ngayFixed;
        private Integer namFixed;
        private Integer namLuyKeThang;

        // Kỳ của file (lấy từ dòng dữ liệu hợp lệ ĐẦU TIÊN — mỗi file chỉ đại diện 1 kỳ),
        // dùng để build nhãn hiển thị "Tên file" ở lịch sử upload (VD "Tháng 5/2026").
        private Integer filePeriodNam;
        private Integer filePeriodThang;
        private String filePeriodQuy;

        int totalStaged = 0;
        int totalSkipped = 0;

        RowHandler(String fileName) {
            this.fileName = fileName;
        }

        void beginSheet(String sheetName) {
            this.sheetName = truncateSheetName(sheetName);
            this.headerIdx = null;
            this.loaiKy = null;
        }

        void flush() {
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(STAGING_INSERT_SQL, batch);
                batch.clear();
            }
        }

        @Override
        public void startRow(int rowNum) {
            currentRow.clear();
        }

        @Override
        public void endRow(int rowNum) {
            if (headerIdx == null) {
                processHeaderRow();
            } else {
                processDataRow();
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (cellReference == null || formattedValue == null) return;
            int col = new CellReference(cellReference).getCol();
            currentRow.put(col, formattedValue);
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // không dùng
        }

        private void processHeaderRow() {
            if (currentRow.isEmpty()) {
                throw new HeaderValidationException("File rỗng, không tìm thấy dòng tiêu đề");
            }
            Map<String, Integer> idx = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> e : currentRow.entrySet()) {
                String norm = normalize(e.getValue());
                if (norm != null) idx.put(norm, e.getKey());
            }

            LoaiKy detected;
            if (idx.containsKey("Tháng")) detected = LoaiKy.THANG;
            else if (idx.containsKey("Quý")) detected = LoaiKy.QUY;
            else if (idx.containsKey("Năm")) detected = LoaiKy.NAM;
            else if (idx.containsKey("Mã - Tên AM")) detected = LoaiKy.NGAY;
            else throw new HeaderValidationException(
                    "Không nhận diện được loại file MPA (thiếu cột Tháng/Quý/Năm/Mã - Tên AM)");

            List<String> missing = new ArrayList<>();
            for (SplitColumnDef c : SPLIT_COLUMNS) {
                if (!idx.containsKey(normalize(c.header()))) missing.add(c.header());
            }
            for (ColumnDef c : SIMPLE_COLUMNS) {
                if (!idx.containsKey(normalize(c.header()))) missing.add(c.header());
            }
            if (!missing.isEmpty()) {
                throw new HeaderValidationException("Thiếu cột: " + String.join(", ", missing));
            }

            this.headerIdx = idx;
            this.loaiKy = detected;
            if (detected == LoaiKy.NGAY) {
                this.ngayFixed = LocalDate.now().minusDays(1);
                this.namFixed = ngayFixed.getYear();
            }
            if (detected == LoaiKy.NAM) {
                this.namLuyKeThang = parseThangFromFileName(fileName);
                if (this.namLuyKeThang == null) {
                    throw new HeaderValidationException(
                            "Không xác định được tháng lũy kế từ tên file. Tên file cần chứa 'T<tháng>', " +
                            "ví dụ: MPA 2026 (lũy kế đến T5).xlsx");
                }
            }
        }

        private void processDataRow() {
            if (currentRow.isEmpty()) return; // dòng trắng — bỏ qua, không tính lỗi

            LocalDate ngay = null;
            Integer thang = null;
            String quy = null;
            Integer nam;

            switch (loaiKy) {
                case THANG -> {
                    Matcher m = THANG_PATTERN.matcher(nullToEmpty(getVal("Tháng")));
                    if (!m.matches()) { totalSkipped++; return; }
                    nam = Integer.parseInt(m.group(1));
                    thang = Integer.parseInt(m.group(2));
                    if (thang < 1 || thang > 12) { totalSkipped++; return; }
                }
                case QUY -> {
                    Matcher m = QUY_PATTERN.matcher(nullToEmpty(getVal("Quý")));
                    if (!m.matches()) { totalSkipped++; return; }
                    nam = Integer.parseInt(m.group(1));
                    quy = "Q" + m.group(2);
                }
                case NAM -> {
                    Integer parsed = parseIntSafe(getVal("Năm"));
                    if (parsed == null) { totalSkipped++; return; }
                    nam = parsed;
                    thang = namLuyKeThang; // mốc "lũy kế đến tháng" tách từ tên file
                }
                case NGAY -> {
                    ngay = ngayFixed;
                    nam = namFixed;
                }
                default -> { totalSkipped++; return; }
            }

            if (filePeriodNam == null) {
                filePeriodNam = nam;
                filePeriodThang = thang;
                filePeriodQuy = quy;
            }

            Object[] simpleValues = new Object[SIMPLE_COLUMNS.size()];
            for (int i = 0; i < SIMPLE_COLUMNS.size(); i++) {
                ColumnDef c = SIMPLE_COLUMNS.get(i);
                String raw = getVal(c.header());
                simpleValues[i] = switch (c.type()) {
                    case TEXT, TEXT_NUM_SAFE -> textOrNull(raw);
                    case DECIMAL -> parseDecimal(raw);
                };
            }
            if (simpleValues[0] == null) { totalSkipped++; return; } // ma_kh_cif rỗng

            String[][] splitValues = new String[SPLIT_COLUMNS.size()][];
            for (int i = 0; i < SPLIT_COLUMNS.size(); i++) {
                splitValues[i] = splitMaTen(getVal(SPLIT_COLUMNS.get(i).header()));
            }

            Object[] row = new Object[24];
            row[0] = loaiKy.name();
            row[1] = ngay;
            row[2] = thang;
            row[3] = quy;
            row[4] = nam;
            int p = 5;
            for (String[] sv : splitValues) {
                row[p++] = sv[0];
                row[p++] = sv[1];
            }
            for (Object v : simpleValues) {
                row[p++] = v;
            }
            row[p] = sheetName;

            batch.add(row);
            totalStaged++;
            if (batch.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(STAGING_INSERT_SQL, batch);
                batch.clear();
            }
        }

        private String getVal(String header) {
            Integer col = headerIdx.get(normalize(header));
            return col == null ? null : currentRow.get(col);
        }

        /** Nhãn kỳ thân thiện hiển thị ở cột "Tên file" của lịch sử upload, VD "Tháng 5/2026". */
        String buildKyLabel() {
            if (loaiKy == null || filePeriodNam == null) return null;
            return switch (loaiKy) {
                case THANG -> filePeriodThang != null ? "Tháng " + filePeriodThang + "/" + filePeriodNam : null;
                case QUY -> filePeriodQuy != null
                        ? "Quý " + filePeriodQuy.replace("Q", "") + "/" + filePeriodNam
                        : null;
                case NAM -> "Năm " + filePeriodNam + " (lũy kế đến T" + namLuyKeThang + ")";
                case NGAY -> ngayFixed != null
                        ? "Ngày " + String.format("%02d/%02d/%04d",
                                ngayFixed.getDayOfMonth(), ngayFixed.getMonthValue(), ngayFixed.getYear())
                        : null;
            };
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String textOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Tìm token "T<số>" CUỐI CÙNG trong tên file (VD "...lũy kế đến T5).xlsx" → 5); null nếu không hợp lệ. */
    private static Integer parseThangFromFileName(String fileName) {
        if (fileName == null) return null;
        Matcher m = LUY_KE_THANG_PATTERN.matcher(fileName);
        Integer last = null;
        while (m.find()) {
            try {
                int v = Integer.parseInt(m.group(1));
                if (v >= 1 && v <= 12) last = v;
            } catch (NumberFormatException ignored) {
                // bỏ qua, thử match tiếp theo
            }
        }
        return last;
    }

    private static String truncateSheetName(String name) {
        if (name == null) return null;
        return name.length() > 20 ? name.substring(0, 20) : name;
    }

    /** Trả về {ma, ten}; tách trên dấu " - " ĐẦU TIÊN (tên có thể chứa " - "). */
    private static String[] splitMaTen(String raw) {
        String t = textOrNull(raw);
        if (t == null) return new String[]{null, null};
        int idx = t.indexOf(" - ");
        if (idx < 0) return new String[]{t, null};
        return new String[]{t.substring(0, idx).trim(), t.substring(idx + 3).trim()};
    }

    private static Integer parseIntSafe(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e1) {
            try {
                return (int) Double.parseDouble(t);
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }

    private static BigDecimal parseDecimal(String formattedValue) {
        if (formattedValue == null || formattedValue.isBlank()) return null;
        String s = formattedValue.trim().replace(",", "");
        try {
            return new BigDecimal(s).setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
