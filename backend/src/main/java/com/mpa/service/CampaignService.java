package com.mpa.service;

import com.mpa.dto.CampaignPreviewResponse;
import com.mpa.dto.CampaignRequest;
import com.mpa.dto.CampaignResponse;
import com.mpa.dto.JobRunResult;

import java.util.List;
import java.util.Map;

public interface CampaignService {

    List<CampaignResponse> getAll();

    CampaignResponse getById(Integer id);

    CampaignResponse create(CampaignRequest request);

    CampaignResponse update(Integer id, CampaignRequest request);

    void delete(Integer id);

    CampaignPreviewResponse preview(Integer id);

    /** Xem trước theo tiêu chí chưa lưu (dùng khi đang soạn chiến dịch, chưa bấm Lưu). */
    CampaignPreviewResponse previewByCriteria(CampaignRequest request);

    JobRunResult send(Integer id, boolean testMode);

    Map<String, List<String>> getCriteriaOptions();
}
