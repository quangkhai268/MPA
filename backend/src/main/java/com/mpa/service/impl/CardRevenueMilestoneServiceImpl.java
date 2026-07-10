package com.mpa.service.impl;

import com.mpa.dto.CardRevenueMilestoneRequest;
import com.mpa.dto.CardRevenueMilestoneResponse;
import com.mpa.entity.CardRevenueMilestone;
import com.mpa.repository.CardRevenueMilestoneRepository;
import com.mpa.service.CardRevenueMilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardRevenueMilestoneServiceImpl implements CardRevenueMilestoneService {

    private final CardRevenueMilestoneRepository repo;

    @Override
    public List<CardRevenueMilestoneResponse> getAll() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(CardRevenueMilestone::getSoNgayTuPhatHanh))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CardRevenueMilestoneResponse create(CardRevenueMilestoneRequest req) {
        CardRevenueMilestone entity = new CardRevenueMilestone();
        applyRequest(entity, req);
        entity.setCreatedAt(LocalDateTime.now());
        repo.save(entity);
        return toResponse(entity);
    }

    @Override
    public CardRevenueMilestoneResponse update(Integer id, CardRevenueMilestoneRequest req) {
        CardRevenueMilestone entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mốc doanh số id=" + id));
        applyRequest(entity, req);
        repo.save(entity);
        return toResponse(entity);
    }

    @Override
    public void delete(Integer id) {
        repo.deleteById(id);
    }

    private void applyRequest(CardRevenueMilestone entity, CardRevenueMilestoneRequest req) {
        entity.setSoNgayTuPhatHanh(req.getSoNgayTuPhatHanh());
        entity.setNguongDoanhSo(req.getNguongDoanhSo());
        entity.setMoTa(req.getMoTa());
        entity.setActive(req.getActive() != null ? req.getActive() : true);
    }

    private CardRevenueMilestoneResponse toResponse(CardRevenueMilestone e) {
        return CardRevenueMilestoneResponse.builder()
                .id(e.getId())
                .soNgayTuPhatHanh(e.getSoNgayTuPhatHanh())
                .nguongDoanhSo(e.getNguongDoanhSo())
                .moTa(e.getMoTa())
                .active(e.getActive())
                .build();
    }
}
