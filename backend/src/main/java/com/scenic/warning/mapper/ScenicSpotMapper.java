package com.scenic.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scenic.warning.entity.ScenicSpot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScenicSpotMapper extends BaseMapper<ScenicSpot> {

    /**
     * 查询所有开放的景区及其当前客流
     */
    @Select("SELECT * FROM scenic_spot WHERE status = 1 AND deleted = 0")
    List<ScenicSpot> selectActiveSpots();
}
