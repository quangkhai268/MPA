package com.mpa.service;

import com.mpa.dto.JobRunResult;

public interface CardNotificationService {

    /** Báo cáo & gửi email nhắc khách hàng chưa kích hoạt thẻ. */
    JobRunResult processChuaKichHoat(boolean testMode);

    /** Báo cáo & gửi email nhắc khách hàng đã kích hoạt nhưng chưa phát sinh giao dịch. */
    JobRunResult processChuaPsgd(boolean testMode);

    /**
     * Gửi THỬ đúng 1 email "chưa kích hoạt" (dùng thẻ đầu tiên đủ điều kiện) để kiểm tra mẫu
     * email/SMTP — bỏ qua dedup, không ảnh hưởng tới lịch chạy job thật.
     */
    EmailService.EmailSendResult testSendChuaKichHoatOne();
}
