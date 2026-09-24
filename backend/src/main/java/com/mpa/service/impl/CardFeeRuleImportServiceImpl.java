package com.mpa.service.impl;

import com.mpa.dto.FileImportResult;
import com.mpa.service.CardFeeRuleImportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Import file "DS_PTN_*.xlsx/.xls" — 2 sheet:
 *  - Sheet 1 "PTN": quy tắc phí thường niên theo product_code, dùng ở LATERAL JOIN trong view
 *    v_the_phat_hanh (xem backend/sql/the_phat_hanh_ngay_thu_phi_tiep_theo.sql) để suy ra
 *    doanh_so_mien_ptn hiển thị ở màn quan-ly-the. Bảng card_fee_rule KHÔNG có khóa nghiệp vụ
 *    ổn định — nhiều dòng có thể cùng "code" nhưng khác thuoc_client_code/khac_client_code/
 *    so_tien (VD mã PJC0043M có 3 quy tắc khác nhau tùy nhóm khách hàng).
 *  - Sheet 2 "Client_code": danh mục nhóm khách hàng (client_code/client_name), dùng ở view
 *    trên qua LEFT JOIN card_client_code ON client_name = nhom_kh_the.
 * Cả 2 bảng đều THAY THẾ TOÀN BỘ mỗi lần tải lên (TRUNCATE + insert lại), không upsert — cùng
 * 1 transaction để 2 bảng tham chiếu lẫn nhau (card_fee_rule.thuoc_client_code trỏ tới
 * card_client_code.client_code) luôn nhất quán sau mỗi lần import, không rơi vào trạng thái
 * nửa cũ nửa mới nếu 1 trong 2 sheet lỗi.
 */
@Service
@RequiredArgsConstructor
public class CardFeeRuleImportServiceImpl implements CardFeeRuleImportService {

    private static final String LOAI_FILE = "CARD_FEE_RULE";

    private static final String HEADER_CODE = "CODE";
    private static final String HEADER_NAME = "NAME";
    private static final String HEADER_SO_TIEN = "SO_TIEN";
    // Các cột dưới đây map nếu có trong file, không bắt buộc — không tham gia logic JOIN của
    // view (chỉ code/thuoc_client_code/khac_client_code/so_tien mới dùng ở đó), nên lệch tên
    // nhẹ so với mẫu không làm hỏng cả lần import.
    private static final String HEADER_SP = "SP";
    private static final String HEADER_TARIFF = "TARIFF";
    private static final String HEADER_THUOC_CLIENT_CODE = "THUOC_CLIENT_CODE";
    private static final String HEADER_KHAC_CLIENT_CODE = "KHAC_CLIENT_CODE";
    private static final String HEADER_APPLY_RULES = "APPLY_RULES";
    private static final String HEADER_TARIFF_DOMAIN_OID = "TARIFF_DOMAIN_OID";

    private static final List<String> REQUIRED_HEADERS_PTN = List.of(HEADER_CODE, HEADER_SO_TIEN);
    private static final List<String> REQUIRED_HEADERS_CLIENT_CODE = List.of(HEADER_CODE, HEADER_NAME);

    private final JdbcTemplate jdbcTemplate;

    private record FeeRuleRow(String code, String name, String sp, String tariff, Long soTien,
                               String thuocClientCode, String khacClientCode, String applyRules,
                               Long tariffDomainOid) {}

    private record ClientCodeRow(String clientCode, String clientName) {}

    @Override
    @Transactional
    public FileImportResult importFile(InputStream excelStream, String fileName) {
        try (Workbook wb = WorkbookFactory.create(excelStream)) {
            List<FeeRuleRow> feeRules = new ArrayList<>();
            int feeRuleSkipped = 0;
            {
                Sheet sheet = wb.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    return new FileImportResult(fileName, LOAI_FILE, 0, "FAILED", "Sheet PTN rỗng, không có dòng tiêu đề", null);
                }
                Map<String, Integer> headerIndex = buildHeaderIndex(headerRow);
                List<String> missing = REQUIRED_HEADERS_PTN.stream()
                        .filter(h -> !headerIndex.containsKey(h)).collect(Collectors.toList());
                if (!missing.isEmpty()) {
                    return new FileImportResult(fileName, LOAI_FILE, 0, "FAILED",
                            "Sheet PTN thiếu cột: " + String.join(", ", missing), null);
                }

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    String code = getCellAsString(getCell(row, headerIndex, HEADER_CODE));
                    if (code == null) { feeRuleSkipped++; continue; }

                    feeRules.add(new FeeRuleRow(
                            code,
                            getCellAsString(getCell(row, headerIndex, HEADER_NAME)),
                            getCellAsString(getCell(row, headerIndex, HEADER_SP)),
                            getCellAsString(getCell(row, headerIndex, HEADER_TARIFF)),
                            parseLong(getCellAsString(getCell(row, headerIndex, HEADER_SO_TIEN))),
                            getCellAsString(getCell(row, headerIndex, HEADER_THUOC_CLIENT_CODE)),
                            getCellAsString(getCell(row, headerIndex, HEADER_KHAC_CLIENT_CODE)),
                            getCellAsString(getCell(row, headerIndex, HEADER_APPLY_RULES)),
                            parseLong(getCellAsString(getCell(row, headerIndex, HEADER_TARIFF_DOMAIN_OID)))));
                }
                if (feeRules.isEmpty()) {
                    return new FileImportResult(fileName, LOAI_FILE, 0, "FAILED", "Sheet PTN không có dòng dữ liệu hợp lệ", null);
                }
            }

            List<ClientCodeRow> clientCodes = new ArrayList<>();
            int clientCodeSkipped = 0;
            String clientCodeNote;
            if (wb.getNumberOfSheets() > 1) {
                Sheet sheet = wb.getSheetAt(1);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    clientCodeNote = "sheet Client_code rỗng, bỏ qua";
                } else {
                    Map<String, Integer> headerIndex = buildHeaderIndex(headerRow);
                    List<String> missing = REQUIRED_HEADERS_CLIENT_CODE.stream()
                            .filter(h -> !headerIndex.containsKey(h)).collect(Collectors.toList());
                    if (!missing.isEmpty()) {
                        clientCodeNote = "sheet Client_code thiếu cột " + String.join(", ", missing) + ", bỏ qua";
                    } else {
                        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                            Row row = sheet.getRow(r);
                            if (row == null) continue;
                            String code = getCellAsString(getCell(row, headerIndex, HEADER_CODE));
                            String name = getCellAsString(getCell(row, headerIndex, HEADER_NAME));
                            if (code == null || name == null) { clientCodeSkipped++; continue; }
                            clientCodes.add(new ClientCodeRow(code, name));
                        }
                        clientCodeNote = null;
                    }
                }
            } else {
                clientCodeNote = "không có sheet Client_code, bỏ qua";
            }

            jdbcTemplate.execute("TRUNCATE TABLE card_fee_rule");
            jdbcTemplate.batchUpdate(
                    "INSERT INTO card_fee_rule (code, name, sp, tariff, so_tien, thuoc_client_code, khac_client_code, apply_rules, tariff_domain_oid) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    feeRules, feeRules.size(), (ps, p) -> {
                        ps.setString(1, p.code());
                        ps.setString(2, p.name());
                        ps.setString(3, p.sp());
                        ps.setString(4, p.tariff());
                        if (p.soTien() != null) ps.setLong(5, p.soTien()); else ps.setNull(5, Types.NUMERIC);
                        ps.setString(6, p.thuocClientCode());
                        ps.setString(7, p.khacClientCode());
                        ps.setString(8, p.applyRules());
                        if (p.tariffDomainOid() != null) ps.setLong(9, p.tariffDomainOid()); else ps.setNull(9, Types.BIGINT);
                    });

            if (!clientCodes.isEmpty()) {
                jdbcTemplate.execute("TRUNCATE TABLE card_client_code");
                jdbcTemplate.batchUpdate(
                        "INSERT INTO card_client_code (client_code, client_name) VALUES (?, ?)",
                        clientCodes, clientCodes.size(), (ps, p) -> {
                            ps.setString(1, p.clientCode());
                            ps.setString(2, p.clientName());
                        });
            }

            StringBuilder ghiChu = new StringBuilder();
            ghiChu.append("PTN: thay thế toàn bộ — ").append(feeRules.size()).append(" dòng quy tắc");
            if (feeRuleSkipped > 0) ghiChu.append(" (").append(feeRuleSkipped).append(" dòng bỏ qua, thiếu CODE)");
            if (!clientCodes.isEmpty()) {
                ghiChu.append("; Client_code: thay thế toàn bộ — ").append(clientCodes.size()).append(" dòng");
                if (clientCodeSkipped > 0) ghiChu.append(" (").append(clientCodeSkipped).append(" dòng bỏ qua, thiếu CODE/NAME)");
            } else if (clientCodeNote != null) {
                ghiChu.append("; Client_code: ").append(clientCodeNote);
            }

            return new FileImportResult(fileName, LOAI_FILE, feeRules.size() + clientCodes.size(), "SUCCESS", ghiChu.toString(), null);
        } catch (Exception e) {
            return new FileImportResult(fileName, LOAI_FILE, 0, "FAILED", "Lỗi xử lý file: " + e.getMessage(), null);
        }
    }

    private static Map<String, Integer> buildHeaderIndex(Row headerRow) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (Cell c : headerRow) {
            String h = getCellAsString(c);
            if (h != null) headerIndex.put(h.trim().toUpperCase(), c.getColumnIndex());
        }
        return headerIndex;
    }

    private static Cell getCell(Row row, Map<String, Integer> headerIndex, String header) {
        Integer idx = headerIndex.get(header);
        return idx == null ? null : row.getCell(idx);
    }

    private static Long parseLong(String s) {
        if (s == null) return null;
        String cleaned = s.trim().replace(",", "").replace("'", "");
        if (cleaned.isEmpty()) return null;
        try { return (long) Double.parseDouble(cleaned); } catch (Exception e) { return null; }
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
