package com.mpa.repository;

import com.mpa.entity.UploadHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {
    Page<UploadHistory> findAllByOrderByThoiGianDesc(Pageable pageable);
}
