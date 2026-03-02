package com.scenic.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scenic.warning.entity.ThresholdConfig;
import com.scenic.warning.mapper.ThresholdConfigMapper;
import com.scenic.warning.service.ThresholdConfigService;
import org.springframework.stereotype.Service;

@Service
public class ThresholdConfigServiceImpl extends ServiceImpl<ThresholdConfigMapper, ThresholdConfig>
        implements ThresholdConfigService {

    @Override
    public ThresholdConfig getByScenicId(Long scenicId) {
        LambdaQueryWrapper<ThresholdConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ThresholdConfig::getScenicId, scenicId);
        ThresholdConfig config = this.getOne(wrapper);

        // 如果没有配置，创建默认配置
        if (config == null) {
            config = new ThresholdConfig();
            config.setScenicId(scenicId);
            config.setYellowPercent(new java.math.BigDecimal("80.00"));
            config.setRedPercent(new java.math.BigDecimal("100.00"));
            config.setEnableWarning(1);
            config.setNotifyMethod("WEBSOCKET");
            this.save(config);
        }

        return config;
    }
}
