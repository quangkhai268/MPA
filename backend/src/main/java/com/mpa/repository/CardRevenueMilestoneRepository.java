package com.mpa.repository;

import com.mpa.entity.CardRevenueMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRevenueMilestoneRepository extends JpaRepository<CardRevenueMilestone, Integer> {
    List<CardRevenueMilestone> findByActiveTrue();
}
