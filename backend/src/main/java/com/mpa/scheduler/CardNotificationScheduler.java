package com.mpa.scheduler;

import com.mpa.dto.JobRunResult;
import com.mpa.service.CardMilestoneEvaluationService;
import com.mpa.service.CardNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardNotificationScheduler {

    private final CardNotificationService service;
    private final CardMilestoneEvaluationService milestoneEvaluationService;

    @Scheduled(cron = "0 0 7 * * *")
    public void runChuaKichHoat() {
        JobRunResult result = service.processChuaKichHoat(false);
        log.info("Job chưa kích hoạt: {}/{} đã gửi, {} bỏ qua trùng, {} tắt gửi, {} lỗi",
                result.getSent(), result.getEligible(), result.getSkippedDedup(), result.getSkippedDisabled(), result.getFailed());
    }

    @Scheduled(cron = "0 15 7 * * *")
    public void runChuaPsgd() {
        JobRunResult result = service.processChuaPsgd(false);
        log.info("Job chưa PSGD: {}/{} đã gửi, {} bỏ qua trùng, {} tắt gửi, {} lỗi",
                result.getSent(), result.getEligible(), result.getSkippedDedup(), result.getSkippedDisabled(), result.getFailed());
    }

    @Scheduled(cron = "0 30 7 * * *")
    public void runDoanhSoMoc() {
        JobRunResult result = milestoneEvaluationService.evaluateAndNotify(false);
        log.info("Job mốc doanh số: {}/{} đã gửi, {} bỏ qua trùng, {} tắt gửi, {} lỗi",
                result.getSent(), result.getEligible(), result.getSkippedDedup(), result.getSkippedDisabled(), result.getFailed());
    }
}
