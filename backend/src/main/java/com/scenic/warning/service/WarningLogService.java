package com.scenic.warning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scenic.warning.entity.WarningLog;

public interface WarningLogService extends IService<WarningLog> {

    /**
     * 记录一条预警日志
     */
    void logWarning(Long scenicId, String scenicName, String level,
                    int currentCount, int maxCapacity,
                    double thresholdPercent, double congestionRate);
}
