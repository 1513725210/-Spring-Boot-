package com.scenic.warning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.warning.common.Result;
import com.scenic.warning.entity.ThresholdConfig;
import com.scenic.warning.service.ThresholdConfigService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 预警阈值配置 Controller
 */
@RestController
@RequestMapping("/api/threshold")
public class ThresholdConfigController {

    @Resource
    private ThresholdConfigService thresholdConfigService;

    /**
     * 获取所有阈值配置
     */
    @GetMapping("/list")
    public Result<List<ThresholdConfig>> list() {
        return Result.success(thresholdConfigService.list());
    }

    /**
     * 分页查询阈值配置
     */
    @GetMapping("/page")
    public Result<Page<ThresholdConfig>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ThresholdConfig> page = new Page<>(pageNum, pageSize);
        return Result.success(thresholdConfigService.page(page));
    }

    /**
     * 根据景区ID获取阈值
     */
    @GetMapping("/scenic/{scenicId}")
    public Result<ThresholdConfig> getByScenicId(@PathVariable Long scenicId) {
        return Result.success(thresholdConfigService.getByScenicId(scenicId));
    }

    /**
     * 更新阈值配置
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody ThresholdConfig config) {
        return Result.success(thresholdConfigService.updateById(config));
    }
}
