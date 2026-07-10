package com.mpa.scheduler;

import com.mpa.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotScheduler {

    private final SnapshotService snapshotService;

    @Scheduled(cron = "0 30 6 * * *")
    public void runDailySnapshot() {
        int count = snapshotService.runDailySnapshot();
        log.info("Đã snapshot doanh số cho {} thẻ", count);
    }
}
