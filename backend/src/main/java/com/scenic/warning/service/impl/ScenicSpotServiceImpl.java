package com.scenic.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scenic.warning.entity.ScenicSpot;
import com.scenic.warning.mapper.ScenicSpotMapper;
import com.scenic.warning.service.ScenicSpotService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScenicSpotServiceImpl extends ServiceImpl<ScenicSpotMapper, ScenicSpot>
        implements ScenicSpotService {

    @Override
    public List<ScenicSpot> getActiveSpots() {
        return baseMapper.selectActiveSpots();
    }

    @Override
    public void updateCurrentCount(Long scenicId, int currentCount) {
        LambdaUpdateWrapper<ScenicSpot> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ScenicSpot::getId, scenicId)
               .set(ScenicSpot::getCurrentCount, currentCount);
        this.update(wrapper);
    }
}
