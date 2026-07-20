package com.mpa.service.impl;

import com.mpa.dto.FileImportResult;
import com.mpa.service.ThePhatHanhImportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc file Excel ISS_02 (danh sách thẻ đã phát hành) và nạp vào bảng the_phat_hanh.
 * ISS_02 là bản trích xuất ĐẦY ĐỦ mỗi kỳ (không phải incremental) — theo đúng bản chất
 * "the_phat_hanh chỉ lưu trạng thái hiện tại, không có lịch sử" đã ghi trong tài liệu dự án.
 * Vì vậy luồng nạp dùng staging table: stage toàn bộ trước, chỉ khi thành công mới
 * TRUNCATE bảng sống rồi nạp lại từ staging trong 1 transaction — dữ liệu cũ không bao giờ
 * bị xóa nếu quá trình đọc/stage file mới thất bại giữa chừng.
 */
@Service
@RequiredArgsConstructor
public class ThePhatHanhImportServiceImpl implements ThePhatHanhImportService {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 500;

    private static final DateTimeFormatter[] DATETIME_FORMATS = {
            DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    };
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    };

    private enum ColType { TEXT, TEXT_NUM_SAFE, INTEGER, DECIMAL, DATE, TIMESTAMP }

    private record ColumnDef(String header, String dbColumn, ColType type) {}

    // Thứ tự đúng bằng thứ tự cột trong file ISS_02 gốc — cũng là thứ tự insert vào staging.
    private static final List<ColumnDef> COLUMNS = List.of(
            new ColumnDef("CN QLT", "cn_qlt", ColType.TEXT_NUM_SAFE),
            new ColumnDef("Số CIF khách hàng PHT", "so_cif_khach_hang_pht", ColType.TEXT_NUM_SAFE),
            new ColumnDef("Họ tên khách hàng PHT", "ho_ten_khach_hang_pht", ColType.TEXT),
            new ColumnDef("Số GTTT", "so_gttt", ColType.TEXT),
            new ColumnDef("Thời hạn hiệu lực GTTT", "thoi_han_hieu_luc_gttt", ColType.TEXT),
            new ColumnDef("Thời hạn cư trú", "thoi_han_cu_tru", ColType.TEXT),
            new ColumnDef("Sinh trắc học khách hàng", "sinh_trac_hoc_khach_hang", ColType.TEXT),
            new ColumnDef("SDT", "sdt", ColType.TEXT),
            new ColumnDef("Email", "email", ColType.TEXT),
            new ColumnDef("Liab/Top contract", "liab_top_contract", ColType.TEXT),
            new ColumnDef("Thời hạn HMTD", "thoi_han_hmtd", ColType.DATE),
            new ColumnDef("Issuing contract Nbr", "issuing_contract_nbr", ColType.TEXT),
            new ColumnDef("Product code", "product_code", ColType.TEXT),
            new ColumnDef("Trạng thái Issuing Contract", "trang_thai_issuing_contract", ColType.TEXT),
            new ColumnDef("CIF chủ thẻ chính", "cif_chu_the_chinh", ColType.TEXT_NUM_SAFE),
            new ColumnDef("Tên chủ thẻ chính", "ten_chu_the_chinh", ColType.TEXT),
            new ColumnDef("HMTD - Issuing contract", "hmtd_issuing_contract", ColType.DECIMAL),
            new ColumnDef("AM - Issuing Contract", "am_issuing_contract", ColType.TEXT_NUM_SAFE),
            new ColumnDef("Loại thẻ", "loai_the", ColType.TEXT),
            new ColumnDef("Số thẻ đã phát hành", "so_the_da_phat_hanh", ColType.TEXT),
            new ColumnDef("CARD ID", "card_id", ColType.TEXT_NUM_SAFE),
            new ColumnDef("Trạng thái thẻ", "trang_thai_the", ColType.TEXT),
            new ColumnDef("Lý do phát hành", "ly_do_phat_hanh", ColType.TEXT),
            new ColumnDef("Hình thức thẻ", "hinh_thuc_the", ColType.TEXT),
            new ColumnDef("Plastic status", "plastic_status", ColType.TEXT),
            new ColumnDef("Số ngày chưa kích hoạt", "so_ngay_chua_kich_hoat", ColType.INTEGER),
            new ColumnDef("Ngày phát hành thẻ", "ngay_phat_hanh_the", ColType.TIMESTAMP),
            new ColumnDef("AM - Card", "am_card", ColType.TEXT_NUM_SAFE),
            new ColumnDef("Mã cán bộ giới thiệu", "ma_can_bo_gioi_thieu", ColType.TEXT_NUM_SAFE),
            new ColumnDef("Thời hạn hiệu lực thẻ", "thoi_han_hieu_luc_the", ColType.TEXT),
            new ColumnDef("Doanh số giao dịch để tính miễn PTN", "doanh_so_giao_dich_mien_ptn", ColType.DECIMAL),
            new ColumnDef("Ngày thu phí thường niên gần nhất", "ngay_thu_phi_thuong_nien_gan_nhat", ColType.DATE),
            new ColumnDef("Mức phí thường niên thẻ", "muc_phi_thuong_nien_the", ColType.TEXT),
            new ColumnDef("Số tiền phí thường niên", "so_tien_phi_thuong_nien", ColType.DECIMAL),
            new ColumnDef("Đặc quyền thẻ", "dac_quyen_the", ColType.TEXT),
            new ColumnDef("Kênh phát hành", "kenh_phat_hanh", ColType.TEXT),
            new ColumnDef("Nhóm KH thẻ", "nhom_kh_the", ColType.TEXT),
            new ColumnDef("Ngày cập nhật trạng thái Issuing Contract", "ngay_cap_nhat_trang_thai_issuing_contract", ColType.TIMESTAMP),
            new ColumnDef("Ngày cập nhật trạng thái Card Contract", "ngay_cap_nhat_trang_thai_card_contract", ColType.TIMESTAMP)
    );

    private static final String STAGING_INSERT_SQL;
    static {
        StringBuilder cols = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (ColumnDef c : COLUMNS) {
            if (cols.length() > 0) { cols.append(", "); placeholders.append(", "); }
            cols.append(c.dbColumn());
            placeholders.append("?");
        }
        STAGING_INSERT_SQL = "INSERT INTO the_phat_hanh_staging (" + cols + ") VALUES (" + placeholders + ")";
    }

    @Override
    public void clearStaging() {
        jdbcTemplate.execute("TRUNCATE TABLE the_phat_hanh_staging");
    }

    @Override
    public long countStaged() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM the_phat_hanh_staging", Long.class);
        return count != null ? count : 0;
    }

    @Override
    public FileImportResult stageFile(InputStream excelStream, String fileName) {
        try (Workbook wb = WorkbookFactory.create(excelStream)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return new FileImportResult(fileName, "ISS_02", 0, "FAILED", "File rỗng, không tìm thấy dòng tiêu đề");
            }

            Map<String, Integer> headerIndex = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                String text = getCellAsString(cell);
                if (text != null && !text.isBlank()) headerIndex.put(text.trim(), cell.getColumnIndex());
            }

            List<String> missing = new ArrayList<>();
            for (ColumnDef c : COLUMNS) {
                if (!headerIndex.containsKey(c.header())) missing.add(c.header());
            }
            if (!missing.isEmpty()) {
                return new FileImportResult(fileName, "ISS_02", 0, "FAILED",
                        "Thiếu cột: " + String.join(", ", missing));
            }

            List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
            int totalStaged = 0;
            int totalSkipped = 0;

            int firstDataRow = headerRow.getRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int r = firstDataRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                try {
                    Object[] values = parseRow(row, headerIndex);
                    if (values == null) { totalSkipped++; continue; } // dòng trắng
                    batch.add(values);
                    totalStaged++;
                } catch (Exception ex) {
                    totalSkipped++;
                }

                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(STAGING_INSERT_SQL, batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(STAGING_INSERT_SQL, batch);
            }

            if (totalStaged == 0) {
                return new FileImportResult(fileName, "ISS_02", 0, "FAILED", "Không có dòng dữ liệu hợp lệ");
            }
            String ghiChu = totalSkipped > 0 ? ("Bỏ qua " + totalSkipped + " dòng lỗi/trống") : null;
            return new FileImportResult(fileName, "ISS_02", totalStaged, "SUCCESS", ghiChu);

        } catch (Exception e) {
            return new FileImportResult(fileName, "ISS_02", 0, "FAILED", "Lỗi đọc file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void commitStagedData() {
        StringBuilder cols = new StringBuilder();
        for (ColumnDef c : COLUMNS) {
            if (cols.length() > 0) cols.append(", ");
            cols.append(c.dbColumn());
        }
        jdbcTemplate.execute("TRUNCATE TABLE the_phat_hanh");
        jdbcTemplate.execute(
                "INSERT INTO the_phat_hanh (" + cols + ", created_at) " +
                "SELECT " + cols + ", now() FROM the_phat_hanh_staging"
        );
        jdbcTemplate.execute("TRUNCATE TABLE the_phat_hanh_staging");
    }

    // ── Row / cell parsing ──────────────────────────────────────────────

    /** Trả về null nếu dòng hoàn toàn trống (bỏ qua, không tính lỗi). */
    private Object[] parseRow(Row row, Map<String, Integer> headerIndex) {
        Integer cardIdIdx = headerIndex.get("CARD ID");
        Integer soTheIdx = headerIndex.get("Số thẻ đã phát hành");
        Cell cardIdCell = cardIdIdx != null ? row.getCell(cardIdIdx) : null;
        Cell soTheCell = soTheIdx != null ? row.getCell(soTheIdx) : null;
        String cardId = getCellAsString(cardIdCell);
        String soThe = getCellAsString(soTheCell);
        if ((cardId == null || cardId.isBlank()) && (soThe == null || soThe.isBlank())) {
            return null;
        }

        Object[] values = new Object[COLUMNS.size()];
        for (int i = 0; i < COLUMNS.size(); i++) {
            ColumnDef c = COLUMNS.get(i);
            Cell cell = row.getCell(headerIndex.get(c.header()));
            values[i] = switch (c.type()) {
                case TEXT, TEXT_NUM_SAFE -> getCellAsString(cell);
                case INTEGER -> getCellAsInteger(cell);
                case DECIMAL -> getCellAsDecimal(cell);
                case DATE -> getCellAsDate(cell);
                case TIMESTAMP -> getCellAsDateTime(cell);
            };
        }
        return values;
    }

    private String getCellAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                String s = cell.getStringCellValue();
                return s == null ? null : (s.isBlank() ? null : s.trim());
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return getCellAsString0(cell.getStringCellValue()); }
                catch (Exception e) { return getCellAsString0(String.valueOf(cell.getNumericCellValue())); }
            default:
                return null;
        }
    }

    private String getCellAsString0(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private Integer getCellAsInteger(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
            String s = getCellAsString(cell);
            if (s == null) return null;
            return (int) Double.parseDouble(s.trim());
        } catch (Exception e) { return null; }
    }

    private BigDecimal getCellAsDecimal(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
            String s = getCellAsString(cell);
            if (s == null) return null;
            return new BigDecimal(s.trim().replace(",", ""));
        } catch (Exception e) { return null; }
    }

    private LocalDate getCellAsDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = getCellAsString(cell);
        if (s == null) return null;
        s = s.trim();
        for (DateTimeFormatter f : DATE_FORMATS) {
            try { return LocalDate.parse(s, f); } catch (Exception ignored) {}
        }
        LocalDateTime dt = tryParseDateTime(s);
        return dt != null ? dt.toLocalDate() : null;
    }

    private LocalDateTime getCellAsDateTime(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }
        String s = getCellAsString(cell);
        if (s == null) return null;
        LocalDateTime dt = tryParseDateTime(s.trim());
        if (dt != null) return dt;
        LocalDate d = getCellAsDate(cell);
        return d != null ? d.atStartOfDay() : null;
    }

    private LocalDateTime tryParseDateTime(String s) {
        for (DateTimeFormatter f : DATETIME_FORMATS) {
            try { return LocalDateTime.parse(s, f); } catch (Exception ignored) {}
        }
        return null;
    }
}
