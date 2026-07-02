package com.mpa.service.impl;

import com.mpa.dto.ThongTinAmRequest;
import com.mpa.dto.ThongTinAmResponse;
import com.mpa.entity.PhongBan;
import com.mpa.entity.ThongTinAm;
import com.mpa.repository.PhongBanRepository;
import com.mpa.repository.ThongTinAmRepository;
import com.mpa.service.ThongTinAmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThongTinAmServiceImpl implements ThongTinAmService {

    private final ThongTinAmRepository repo;
    private final PhongBanRepository phongBanRepo;

    @Override
    public Page<ThongTinAmResponse> getList(String search, Short trangThai, int page, int size) {
        Map<String, PhongBan> phongMap = buildPhongMap();
        String s = search == null ? "" : search.trim();
        if (trangThai == null) {
            return repo.searchAll(s, PageRequest.of(page, size)).map(t -> toResponse(t, phongMap));
        }
        return repo.searchByStatus(s, trangThai, PageRequest.of(page, size)).map(t -> toResponse(t, phongMap));
    }

    @Override
    public List<ThongTinAmResponse> getAll(String search, Short trangThai) {
        Map<String, PhongBan> phongMap = buildPhongMap();
        String s = search == null ? "" : search.trim();
        List<ThongTinAm> list = trangThai == null
            ? repo.searchAllList(s)
            : repo.searchByStatusList(s, trangThai);
        return list.stream().map(t -> toResponse(t, phongMap)).collect(Collectors.toList());
    }

    @Override
    public ThongTinAmResponse create(ThongTinAmRequest req) {
        ThongTinAm entity = new ThongTinAm();
        applyRequest(entity, req);
        repo.save(entity);
        return toResponse(entity, buildPhongMap());
    }

    @Override
    public ThongTinAmResponse update(Integer id, ThongTinAmRequest req) {
        ThongTinAm entity = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy cán bộ AM id=" + id));
        applyRequest(entity, req);
        repo.save(entity);
        return toResponse(entity, buildPhongMap());
    }

    @Override
    public void delete(Integer id) {
        repo.deleteById(id);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void applyRequest(ThongTinAm entity, ThongTinAmRequest req) {
        entity.setMaCn(req.getMaCn());
        entity.setMaDonViCap6(req.getMaDonViCap6());
        entity.setMaAm(req.getMaAm());
        entity.setTenAm(req.getTenAm());
        entity.setSoDienThoai(req.getSoDienThoai());
        entity.setNgayBatDau(req.getNgayBatDau());
        entity.setEmail(req.getEmail());
        entity.setChucVu(req.getChucVu());
        entity.setLoaiAm(req.getLoaiAm());
        entity.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : 1);
    }

    private ThongTinAmResponse toResponse(ThongTinAm t, Map<String, PhongBan> phongMap) {
        PhongBan phong = (t.getMaDonViCap6() != null) ? phongMap.get(t.getMaDonViCap6()) : null;
        return ThongTinAmResponse.builder()
            .id(t.getId())
            .maCn(t.getMaCn())
            .maDonViCap6(t.getMaDonViCap6())
            .tenPhong(phong != null ? phong.getTenDonViCap6() : null)
            .tenCn(phong != null ? phong.getTenCn() : null)
            .maAm(t.getMaAm())
            .tenAm(t.getTenAm())
            .soDienThoai(t.getSoDienThoai())
            .ngayBatDau(t.getNgayBatDau())
            .email(t.getEmail())
            .chucVu(t.getChucVu())
            .loaiAm(t.getLoaiAm())
            .trangThai(t.getTrangThai())
            .build();
    }

    @Override
    public List<Map<String, String>> getAmDropdown() {
        return repo.findAll().stream()
            .filter(t -> t.getMaAm() != null && (t.getTrangThai() == null || t.getTrangThai() == 1))
            .collect(Collectors.toMap(
                ThongTinAm::getMaAm,
                t -> t,
                (a, b) -> a
            ))
            .values().stream()
            .sorted(Comparator.comparing(t -> t.getTenAm() == null ? "" : t.getTenAm()))
            .map(t -> Map.of("ma", t.getMaAm(), "ten", t.getTenAm() != null ? t.getTenAm() : t.getMaAm()))
            .collect(Collectors.toList());
    }

    private Map<String, PhongBan> buildPhongMap() {
        return phongBanRepo.findAll().stream()
            .filter(p -> p.getMaDonViCap6() != null)
            .collect(Collectors.toMap(PhongBan::getMaDonViCap6, p -> p, (a, b) -> a));
    }
}
