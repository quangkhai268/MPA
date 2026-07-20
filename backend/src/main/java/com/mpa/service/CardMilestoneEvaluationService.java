package com.mpa.service;

import com.mpa.dto.JobRunResult;

public interface CardMilestoneEvaluationService {

    /**
     * Với mỗi mốc doanh số đang active, tìm các thẻ phát hành đúng
     * (hôm nay - soNgayTuPhatHanh) ngày trước mà chưa đạt ngưỡng doanh số,
     * gửi email nhắc (1 lần/thẻ/mốc, không lặp lại).
     */
    JobRunResult evaluateAndNotify();
}
