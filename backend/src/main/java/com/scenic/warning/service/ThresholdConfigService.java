package com.scenic.warning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scenic.warning.entity.ThresholdConfig;

public interface ThresholdConfigService extends IService<ThresholdConfig> {

    /**
     * 根据景区ID获取阈值配置
     */
    ThresholdConfig getByScenicId(Long scenicId);
}
