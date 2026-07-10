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

    private record DeltaPoint(LocalDate date, BigDecimal delta) {}

    @Override
    public RevenueSeriesResponse getSeries(String cardId, String granularity, LocalDate from, LocalDate to) {
        List<TheDoanhSoSnapshot> snapshots =
                repo.findByCardIdAndNgaySnapshotBetweenOrderByNgaySnapshot(cardId, from, to);
        List<DeltaPoint> deltas = computeDeltas(snapshots);
        return build(deltas, granularity, snapshots.size() < 2);
    }

    @Override
    public RevenueSeriesResponse getBaoCaoTongHop(String granularity, LocalDate from, LocalDate to) {
        List<TheDoanhSoSnapshot> all = repo.findByNgaySnapshotBetweenOrderByNgaySnapshot(from, to);
        Map<String, List<TheDoanhSoSnapshot>> byCard = new HashMap<>();
        for (TheDoanhSoSnapshot s : all) {
            byCard.computeIfAbsent(s.getCardId(), k -> new ArrayList<>()).add(s);
        }
        List<DeltaPoint> allDeltas = new ArrayList<>();
        for (List<TheDoanhSoSnapshot> cardSnapshots : byCard.values()) {
            cardSnapshots.sort(Comparator.comparing(TheDoanhSoSnapshot::getNgaySnapshot));
            allDeltas.addAll(computeDeltas(cardSnapshots));
        }
        boolean chuaDuLieu = byCard.values().stream().noneMatch(list -> list.size() >= 2);
        return build(allDeltas, granularity, chuaDuLieu);
    }

    private List<DeltaPoint> computeDeltas(List<TheDoanhSoSnapshot> sortedByDate) {
        List<DeltaPoint> deltas = new ArrayList<>();
        BigDecimal prev = null;
        for (TheDoanhSoSnapshot s : sortedByDate) {
            BigDecimal current = s.getDoanhSoLuyKe() != null ? s.getDoanhSoLuyKe() : BigDecimal.ZERO;
            if (prev == null) {
                deltas.add(new DeltaPoint(s.getNgaySnapshot(), BigDecimal.ZERO));
            } else {
                BigDecimal delta = current.subtract(prev);
                // Âm coi như reset chu kỳ PTN — clamp về 0 thay vì hiển thị doanh số âm.
                if (delta.compareTo(BigDecimal.ZERO) < 0) delta = BigDecimal.ZERO;
                deltas.add(new DeltaPoint(s.getNgaySnapshot(), delta));
            }
            prev = current;
        }
        return deltas;
    }

    private RevenueSeriesResponse build(List<DeltaPoint> deltas, String granularity, boolean chuaDuLieu) {
        Map<String, BigDecimal> buckets = new LinkedHashMap<>();
        for (DeltaPoint dp : deltas) {
            String key = bucketKey(dp.date(), granularity);
            buckets.merge(key, dp.delta(), BigDecimal::add);
        }
        List<String> sortedKeys = new ArrayList<>(buckets.keySet());
        Collections.sort(sortedKeys);

        List<RevenueSeriesPoint> points = new ArrayList<>();
        BigDecimal tong = BigDecimal.ZERO;
        for (String key : sortedKeys) {
            BigDecimal value = buckets.get(key);
            points.add(new RevenueSeriesPoint(displayLabel(key, granularity), value));
            tong = tong.add(value);
        }

        return RevenueSeriesResponse.builder()
                .points(points)
                .tong(tong)
                .chuaDuLieu(chuaDuLieu)
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
