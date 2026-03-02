package com.scenic.warning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scenic.warning.entity.FlowRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface FlowRecordService extends IService<FlowRecord> {

    /**
     * 查询指定景区在时间范围内的客流记录
     */
    List<FlowRecord> getByTimeRange(Long scenicId, LocalDateTime start, LocalDateTime end);

    /**
     * 获取景区最近N条客流记录
     */
    List<FlowRecord> getLatestRecords(Long scenicId, int limit);
}
