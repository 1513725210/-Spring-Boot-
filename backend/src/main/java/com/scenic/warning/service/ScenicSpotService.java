package com.scenic.warning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scenic.warning.entity.ScenicSpot;

import java.util.List;

public interface ScenicSpotService extends IService<ScenicSpot> {

    /**
     * 获取所有开放的景区
     */
    List<ScenicSpot> getActiveSpots();

    /**
     * 更新景区当前客流人数
     */
    void updateCurrentCount(Long scenicId, int currentCount);
}
