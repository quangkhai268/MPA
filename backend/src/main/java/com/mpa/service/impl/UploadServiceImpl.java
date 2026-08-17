package com.mpa.service.impl;

import com.mpa.dto.DuLieuMpaPeriod;
import com.mpa.dto.FileImportResult;
import com.mpa.dto.UploadBatchResult;
import com.mpa.dto.UploadHistoryResponse;
import com.mpa.entity.UploadHistory;
import com.mpa.repository.UploadHistoryRepository;
import com.mpa.service.BscSyncService;
import com.mpa.service.DuLieuMpaImportService;
import com.mpa.service.SnapshotService;
import com.mpa.service.ThePhatHanhImportService;
import com.mpa.service.ThongTinAmImportService;
import com.mpa.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final ThePhatHanhImportService thePhatHanhImportService;
    private final DuLieuMpaImportService duLieuMpaImportService;
    private final ThongTinAmImportService thongTinAmImportService;
    private final BscSyncService bscSyncService;
    private final SnapshotService snapshotService;
    private final UploadHistoryRepository uploadHistoryRepository;

    private record Entry(String name, byte[] bytes) {}

    @Override
    public UploadBatchResult handleUpload(MultipartFile[] files, LocalDate ngayDuLieu, String nguoiUpload) {
        thePhatHanhImportService.clearStaging();
        duLieuMpaImportService.clearStaging();

        List<FileImportResult> results = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();

        for (MultipartFile f : files) {
            String name = f.getOriginalFilename() != null ? f.getOriginalFilename() : "file";
            try {
                if (name.toLowerCase().endsWith(".zip")) {
                    entries.addAll(extractZip(f.getInputStream()));
                } else {
                    entries.add(new Entry(name, f.getBytes()));
                }
            } catch (IOException e) {
                results.add(new FileImportResult(name, "UNKNOWN", 0, "FAILED", "Lỗi đọc file: " + e.getMessage(), null));
            }
        }

        boolean anyStaged = false;
        boolean anyMpaStaged = false;
        for (Entry e : entries) {
            String type = classify(e.name());
            if ("ISS_02".equals(type)) {
                FileImportResult r = thePhatHanhImportService.stageFile(new ByteArrayInputStream(e.bytes()), e.name());
                results.add(r);
                if ("SUCCESS".equals(r.getTrangThai())) anyStaged = true;
            } else if ("MPA".equals(type)) {
                FileImportResult r = duLieuMpaImportService.stageFile(new ByteArrayInputStream(e.bytes()), e.name());
                results.add(r);
                if ("SUCCESS".equals(r.getTrangThai())) anyMpaStaged = true;
            } else if ("UNKNOWN".equals(type) && thongTinAmImportService.isThongTinAmFile(e.bytes())) {
                // Không nhận diện được qua tên file — kiểm tra ô tiêu đề đầu tiên của Excel.
                FileImportResult r = thongTinAmImportService.importFile(new ByteArrayInputStream(e.bytes()), e.name());
                results.add(r);
            } else {
                results.add(new FileImportResult(e.name(), type, 0, "UNSUPPORTED", "Chưa hỗ trợ loại file này", null));
            }
        }

        if (anyStaged) {
            thePhatHanhImportService.commitStagedData();
            // Snapshot doanh số ngay sau khi nạp xong — thay cho job chạy theo lịch cố định
            // hằng ngày, vì doanh_so_giao_dich_mien_ptn chỉ thật sự đổi khi có import mới
            // (the_phat_hanh là bảng thay thế toàn bộ, không cập nhật realtime giữa các lần
            // import). Dùng ngayDuLieu (ngày nghiệp vụ dữ liệu đại diện, không phải giờ bấm
            // upload) làm ngày snapshot. Upsert theo (card_id, ngay_snapshot) nên nếu 1 ngày
            // import nhiều lần, lần sau tự ghi đè lần trước — không tạo nhiều dòng trùng ngày.
            snapshotService.runSnapshot(ngayDuLieu);
        }
        if (anyMpaStaged) {
            List<DuLieuMpaPeriod> periods = duLieuMpaImportService.commitStagedData();
            periods.forEach(bscSyncService::syncPeriod);
        }

        int tongDong = 0;
        boolean anyFailed = false;
        boolean anySuccess = false;
        for (FileImportResult r : results) {
            tongDong += r.getSoDong();
            if ("FAILED".equals(r.getTrangThai())) anyFailed = true;
            if ("SUCCESS".equals(r.getTrangThai())) anySuccess = true;
        }

        String trangThai;
        if (anySuccess && !anyFailed) trangThai = "SUCCESS";
        else if (anySuccess) trangThai = "PARTIAL";
        else if (anyFailed) trangThai = "FAILED";
        else trangThai = "UNSUPPORTED";

        UploadHistory h = new UploadHistory();
        h.setThoiGian(LocalDateTime.now());
        h.setNguoiUpload(nguoiUpload);
        h.setNgayDuLieu(ngayDuLieu);
        h.setSoFile(files.length);
        h.setTongDong(tongDong);
        h.setTrangThai(trangThai);
        h.setChiTiet(summarize(results));
        h.setKyLabel(buildKyLabel(results));
        uploadHistoryRepository.save(h);

        return UploadBatchResult.builder()
                .thoiGian(h.getThoiGian())
                .nguoiUpload(nguoiUpload)
                .ngayDuLieu(ngayDuLieu)
                .soFile(files.length)
                .tongDong(tongDong)
                .trangThai(trangThai)
                .files(results)
                .build();
    }

    @Override
    public Page<UploadHistoryResponse> getHistory(Pageable pageable) {
        return uploadHistoryRepository.findAllByOrderByThoiGianDesc(pageable)
                .map(UploadHistoryResponse::from);
    }

    private String classify(String fileName) {
        String upper = fileName.toUpperCase();
        if (upper.contains("ISS_02") || upper.contains("ISS02")) return "ISS_02";
        if (upper.contains("ISS_06") || upper.contains("ISS06")) return "ISS_06";
        if (upper.contains("ISS_15") || upper.contains("ISS15")) return "ISS_15";
        if (upper.contains("MPA")) return "MPA";
        return "UNKNOWN";
    }

    private List<Entry> extractZip(InputStream zipStream) throws IOException {
        List<Entry> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                String lower = name.toLowerCase();
                if (!entry.isDirectory() && (lower.endsWith(".xlsx") || lower.endsWith(".xls"))) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    zis.transferTo(bos);
                    entries.add(new Entry(name, bos.toByteArray()));
                }
                zis.closeEntry();
            }
        }
        return entries;
    }

    /** Nhãn "Tên file" hiển thị ở lịch sử upload — gộp nhãn kỳ (hoặc tên file gốc nếu không có) của các file thành công. */
    private String buildKyLabel(List<FileImportResult> results) {
        String joined = results.stream()
                .filter(r -> "SUCCESS".equals(r.getTrangThai()))
                .map(r -> r.getKyLabel() != null ? r.getKyLabel() : r.getTenFile())
                .collect(Collectors.joining(", "));
        return joined.isEmpty() ? null : joined;
    }

    private String summarize(List<FileImportResult> results) {
        StringBuilder sb = new StringBuilder();
        for (FileImportResult r : results) {
            sb.append(r.getTenFile()).append(" [").append(r.getLoaiFile()).append("]: ")
              .append(r.getTrangThai()).append(" - ").append(r.getSoDong()).append(" dòng");
            if (r.getGhiChu() != null) sb.append(" (").append(r.getGhiChu()).append(")");
            sb.append("\n");
        }
        return sb.toString();
    }
}
