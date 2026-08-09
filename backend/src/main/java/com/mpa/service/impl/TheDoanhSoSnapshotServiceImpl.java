package com.mpa.service.impl;

import com.mpa.dto.RevenueSeriesPoint;
import com.mpa.dto.RevenueSeriesResponse;
import com.mpa.entity.TheDoanhSoSnapshot;
import com.mpa.repository.TheDoanhSoSnapshotRepository;
import com.mpa.service.TheDoanhSoSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TheDoanhSoSnapshotServiceImpl implements TheDoanhSoSnapshotService {

    private final TheDoanhSoSnapshotRepository repo;

    // doanh_so_luy_ke lưu trong the_doanh_so_snapshot LÀ số lũy kế tính đến ngày import (không
    // phải số phát sinh riêng trong kỳ) — vì vậy chart chỉ hiển thị lại đúng giá trị đó theo
    // từng kỳ, KHÔNG trừ lùi giữa 2 lần snapshot để suy ra "phát sinh trong kỳ" như trước.

    @Override
    public RevenueSeriesResponse getSeries(String cardId, String granularity, LocalDate from, LocalDate to) {
        List<TheDoanhSoSnapshot> snapshots;
        if ("ngay".equals(granularity)) {
            // "7 ngày gần nhất" = 7 LẦN snapshot gần nhất của thẻ, không phải 7 ngày dương
            // lịch — vì snapshot giờ chỉ tạo khi có import nên có thể cách nhau nhiều ngày.
            List<TheDoanhSoSnapshot> recent = repo.findFirst7ByCardIdOrderByNgaySnapshotDesc(cardId);
            snapshots = new ArrayList<>(recent);
            Collections.reverse(snapshots);
        } else {
            snapshots = repo.findByCardIdAndNgaySnapshotBetweenOrderByNgaySnapshot(cardId, from, to);
        }
        Map<String, TheDoanhSoSnapshot> lastInBucket = lastSnapshotPerBucket(snapshots, granularity);
        return buildFromLuyKe(lastInBucket, granularity);
    }

    @Override
    public RevenueSeriesResponse getBaoCaoTongHop(String granularity, LocalDate from, LocalDate to) {
        List<TheDoanhSoSnapshot> all = repo.findByNgaySnapshotBetweenOrderByNgaySnapshot(from, to);
        Map<String, List<TheDoanhSoSnapshot>> byCard = new HashMap<>();
        for (TheDoanhSoSnapshot s : all) {
            byCard.computeIfAbsent(s.getCardId(), k -> new ArrayList<>()).add(s);
        }
        // Mỗi thẻ đóng góp giá trị lũy kế tại bản ghi MỚI NHẤT trong từng kỳ (không phải cộng
        // dồn nhiều bản ghi của cùng 1 thẻ trong kỳ đó) — rồi cộng qua tất cả thẻ để ra tổng
        // lũy kế toàn danh mục tại cuối mỗi kỳ.
        Map<String, BigDecimal> bucketTotals = new TreeMap<>();
        for (List<TheDoanhSoSnapshot> cardSnapshots : byCard.values()) {
            Map<String, TheDoanhSoSnapshot> lastInBucket = lastSnapshotPerBucket(cardSnapshots, granularity);
            for (Map.Entry<String, TheDoanhSoSnapshot> e : lastInBucket.entrySet()) {
                BigDecimal v = e.getValue().getDoanhSoLuyKe() != null ? e.getValue().getDoanhSoLuyKe() : BigDecimal.ZERO;
                bucketTotals.merge(e.getKey(), v, BigDecimal::add);
            }
        }
        return buildFromBuckets(bucketTotals, granularity);
    }

    /** Với mỗi kỳ (bucket), chỉ giữ lại bản ghi có ngay_snapshot mới nhất trong kỳ đó. */
    private Map<String, TheDoanhSoSnapshot> lastSnapshotPerBucket(List<TheDoanhSoSnapshot> snapshots, String granularity) {
        Map<String, TheDoanhSoSnapshot> lastInBucket = new TreeMap<>();
        for (TheDoanhSoSnapshot s : snapshots) {
            String key = bucketKey(s.getNgaySnapshot(), granularity);
            TheDoanhSoSnapshot existing = lastInBucket.get(key);
            if (existing == null || s.getNgaySnapshot().isAfter(existing.getNgaySnapshot())) {
                lastInBucket.put(key, s);
            }
        }
        return lastInBucket;
    }

    private RevenueSeriesResponse buildFromLuyKe(Map<String, TheDoanhSoSnapshot> lastInBucket, String granularity) {
        Map<String, BigDecimal> bucketValues = new TreeMap<>();
        for (Map.Entry<String, TheDoanhSoSnapshot> e : lastInBucket.entrySet()) {
            BigDecimal v = e.getValue().getDoanhSoLuyKe() != null ? e.getValue().getDoanhSoLuyKe() : BigDecimal.ZERO;
            bucketValues.put(e.getKey(), v);
        }
        return buildFromBuckets(bucketValues, granularity);
    }

    private RevenueSeriesResponse buildFromBuckets(Map<String, BigDecimal> bucketValues, String granularity) {
        List<RevenueSeriesPoint> points = new ArrayList<>();
        BigDecimal tong = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : bucketValues.entrySet()) {
            points.add(new RevenueSeriesPoint(displayLabel(e.getKey(), granularity), e.getValue()));
            // Các giá trị đều là lũy kế (không phải phát sinh từng kỳ) nên Tổng = giá trị của
            // kỳ gần nhất (bucketValues là TreeMap nên entry cuối cùng luôn là kỳ mới nhất),
            // không cộng dồn qua các kỳ.
            tong = e.getValue();
        }
        return RevenueSeriesResponse.builder()
                .points(points)
                .tong(tong)
                .chuaDuLieu(points.isEmpty())
                .build();
    }

    private String bucketKey(LocalDate date, String granularity) {
        return switch (granularity) {
            case "thang" -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "quy"   -> date.getYear() + "-Q" + (((date.getMonthValue() - 1) / 3) + 1);
            case "nam"   -> String.valueOf(date.getYear());
            default      -> date.format(DateTimeFormatter.ISO_DATE); // "ngay"
        };
    }

    private String displayLabel(String key, String granularity) {
        return switch (granularity) {
            case "thang" -> {
                String[] parts = key.split("-");
                yield parts[1] + "/" + parts[0];
            }
            case "quy" -> {
                String[] parts = key.split("-Q");
                yield "Q" + parts[1] + "/" + parts[0];
            }
            case "nam" -> key;
            default -> {
                LocalDate d = LocalDate.parse(key);
                yield d.format(DateTimeFormatter.ofPattern("dd/MM"));
            }
        };
    }
}
