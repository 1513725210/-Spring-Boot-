package com.scenic.warning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scenic.warning.entity.FlowRecord;
import com.scenic.warning.mapper.FlowRecordMapper;
import com.scenic.warning.service.FlowRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlowRecordServiceImpl extends ServiceImpl<FlowRecordMapper, FlowRecord>
        implements FlowRecordService {

    @Override
    public List<FlowRecord> getByTimeRange(Long scenicId, LocalDateTime start, LocalDateTime end) {
        return baseMapper.selectByTimeRange(scenicId, start, end);
    }

    @Override
    public List<FlowRecord> getLatestRecords(Long scenicId, int limit) {
        return baseMapper.selectLatestRecords(scenicId, limit);
    }
}
