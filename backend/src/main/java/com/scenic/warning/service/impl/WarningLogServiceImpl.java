package com.scenic.warning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scenic.warning.entity.WarningLog;
import com.scenic.warning.mapper.WarningLogMapper;
import com.scenic.warning.service.RagEmergencyService;
import com.scenic.warning.service.WarningLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class WarningLogServiceImpl extends ServiceImpl<WarningLogMapper, WarningLog>
        implements WarningLogService {

    @Autowired
    private RagEmergencyService ragEmergencyService;

    @Override
    public void logWarning(Long scenicId, String scenicName, String level,
                           int currentCount, int maxCapacity,
                           double thresholdPercent, double congestionRate) {
        WarningLog log = new WarningLog();
        log.setScenicId(scenicId);
        log.setScenicName(scenicName);
        log.setWarningLevel(level);
        log.setCurrentCount(currentCount);
        log.setMaxCapacity(maxCapacity);
        log.setThresholdPercent(BigDecimal.valueOf(thresholdPercent));
        log.setCongestionRate(BigDecimal.valueOf(congestionRate));
        log.setWarningTime(LocalDateTime.now());

        String msg = String.format("【%s预警】%s 当前客流 %d 人，拥挤度 %.1f%%，已超过 %.0f%% 阈值",
                "RED".equals(level) ? "红色" : "黄色",
                scenicName, currentCount, congestionRate, thresholdPercent);
        log.setMessage(msg);

        if ("RED".equals(level)) {
            String plan = ragEmergencyService.generateEmergencyPlan(scenicName, level, currentCount, congestionRate);
            log.setPlan(plan);
        }

        this.save(log);
    }
}
