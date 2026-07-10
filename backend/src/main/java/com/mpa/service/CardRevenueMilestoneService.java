package com.mpa.service;

import com.mpa.dto.CardRevenueMilestoneRequest;
import com.mpa.dto.CardRevenueMilestoneResponse;

import java.util.List;

public interface CardRevenueMilestoneService {
    List<CardRevenueMilestoneResponse> getAll();
    CardRevenueMilestoneResponse create(CardRevenueMilestoneRequest request);
    CardRevenueMilestoneResponse update(Integer id, CardRevenueMilestoneRequest request);
    void delete(Integer id);
}
