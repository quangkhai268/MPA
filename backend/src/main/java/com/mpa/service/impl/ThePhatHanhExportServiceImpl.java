package com.mpa.service.impl;

import com.mpa.entity.PhongBan;
import com.mpa.entity.ThePhatHanh;
import com.mpa.entity.ThongTinAm;
import com.mpa.repository.PhongBanRepository;
import com.mpa.repository.ThongTinAmRepository;
import com.mpa.service.ThePhatHanhExportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThePhatHanhExportServiceImpl implements ThePhatHanhExportService {

    private static final String[] HEADERS = {
            "Số thẻ", "Số CardID", "Tên chủ thẻ", "Hạn mức (VND)", "Doanh số (VND)",
            "Mức miễn phí thường niên (VND)", "Mã AM", "Phòng quản lý", "Trạng thái thẻ",
            "Phí thường niên (VND)", "Ngày thu phí thường niên tiếp theo"
    };

    private final ThongTinAmRepository thongTinAmRepo;
    private final PhongBanRepository phongBanRepo;

    @Override
    public byte[] exportExcel(List<ThePhatHanh> cards) {
        // AM -> mã đơn vị cấp 6 -> tên phòng (cùng cách quy đổi dùng cho filter "Phòng ban").
        Map<String, String> maDonViByMaAm = thongTinAmRepo.findAll().stream()
                .filter(a -> a.getMaAm() != null && a.getMaDonViCap6() != null)
                .collect(Collectors.toMap(ThongTinAm::getMaAm, ThongTinAm::getMaDonViCap6, (a, b) -> a));
        Map<String, String> tenPhongByMaDonVi = phongBanRepo.findAll().stream()
                .filter(p -> p.getMaDonViCap6() != null && p.getTenDonViCap6() != null)
                .collect(Collectors.toMap(PhongBan::getMaDonViCap6, PhongBan::getTenDonViCap6, (a, b) -> a));

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Danh sach the");

            CellStyle headerStyle = headerStyle(wb);
            CellStyle numberStyle = numberStyle(wb);
            CellStyle dateStyle = dateStyle(wb);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ThePhatHanh card : cards) {
                Row row = sheet.createRow(rowIdx++);

                setString(row, 0, card.getSoTheDaPhatHanh());
                setString(row, 1, card.getCardId());
                setString(row, 2, nvl(card.getTenChuTheChinh(), card.getHoTenKhachHangPht()));
                setNumber(row, 3, card.getHmtdIssuingContract(), numberStyle);
                setNumber(row, 4, card.getDoanhSoGiaoDichMienPtn(), numberStyle);
                setNumber(row, 5, card.getDoanhSoMienPtn(), numberStyle);
                setString(row, 6, card.getAmIssuingContract());

                String maDonVi = card.getAmIssuingContract() != null ? maDonViByMaAm.get(card.getAmIssuingContract()) : null;
                setString(row, 7, maDonVi != null ? tenPhongByMaDonVi.get(maDonVi) : null);

                setString(row, 8, card.getTrangThaiThe());
                setNumber(row, 9, card.getSoTienPhiThuongNien(), numberStyle);
                setDate(row, 10, card.getNgayThuPhiThuongTienTiepTheo(), dateStyle);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            sheet.setColumnWidth(2, 28 * 256);
            sheet.setColumnWidth(7, 30 * 256);
            sheet.setColumnWidth(10, 26 * 256);
            sheet.createFreezePane(0, 1);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Lỗi khi tạo file Excel", e);
        }
    }

    private static String nvl(String primary, String fallback) {
        return (primary != null && !primary.isBlank()) ? primary : fallback;
    }

    private static void setString(Row row, int col, String value) {
        if (value != null) row.createCell(col).setCellValue(value);
    }

    private static void setNumber(Row row, int col, BigDecimal value, CellStyle style) {
        if (value == null) return;
        Cell cell = row.createCell(col);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private static void setDate(Row row, int col, java.time.LocalDate value, CellStyle style) {
        if (value == null) return;
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static CellStyle headerStyle(XSSFWorkbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle numberStyle(XSSFWorkbook wb) {
        DataFormat fmt = wb.createDataFormat();
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(fmt.getFormat("#,##0"));
        return style;
    }

    private static CellStyle dateStyle(XSSFWorkbook wb) {
        DataFormat fmt = wb.createDataFormat();
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(fmt.getFormat("dd/mm/yyyy"));
        return style;
    }
}
