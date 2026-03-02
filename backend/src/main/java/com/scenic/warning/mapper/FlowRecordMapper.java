package com.scenic.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scenic.warning.entity.FlowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FlowRecordMapper extends BaseMapper<FlowRecord> {

    /**
     * 查询指定景区在时间范围内的客流记录
     */
    @Select("SELECT * FROM flow_record WHERE scenic_id = #{scenicId} " +
            "AND record_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY record_time ASC")
    List<FlowRecord> selectByTimeRange(@Param("scenicId") Long scenicId,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 获取景区最近N条客流记录
     */
    @Select("SELECT * FROM flow_record WHERE scenic_id = #{scenicId} " +
            "ORDER BY record_time DESC LIMIT #{limit}")
    List<FlowRecord> selectLatestRecords(@Param("scenicId") Long scenicId,
                                          @Param("limit") int limit);
}
