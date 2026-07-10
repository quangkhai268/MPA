package com.mpa.repository;

import com.mpa.entity.CampaignCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignCriteriaRepository extends JpaRepository<CampaignCriteria, Integer> {
    List<CampaignCriteria> findByCampaignId(Integer campaignId);
    void deleteByCampaignId(Integer campaignId);
}
