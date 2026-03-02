package com.scenic.warning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.warning.common.Result;
import com.scenic.warning.entity.ScenicSpot;
import com.scenic.warning.service.ScenicSpotService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 景区信息管理 Controller
 */
@RestController
@RequestMapping("/api/scenic")
public class ScenicSpotController {

    @Resource
    private ScenicSpotService scenicSpotService;

    /**
     * 获取所有景区列表
     */
    @GetMapping("/list")
    public Result<List<ScenicSpot>> list() {
        List<ScenicSpot> list = scenicSpotService.list(
                new LambdaQueryWrapper<ScenicSpot>().orderByDesc(ScenicSpot::getLevel)
        );
        return Result.success(list);
    }

    /**
     * 分页查询景区
     */
    @GetMapping("/page")
    public Result<Page<ScenicSpot>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city) {
        Page<ScenicSpot> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(ScenicSpot::getName, name);
        }
        if (city != null && !city.isEmpty()) {
            wrapper.eq(ScenicSpot::getCity, city);
        }
        wrapper.orderByDesc(ScenicSpot::getLevel);
        return Result.success(scenicSpotService.page(page, wrapper));
    }

    /**
     * 获取所有开放景区（大屏使用）
     */
    @GetMapping("/active")
    public Result<List<ScenicSpot>> getActiveSpots() {
        return Result.success(scenicSpotService.getActiveSpots());
    }

    /**
     * 获取景区详情
     */
    @GetMapping("/{id}")
    public Result<ScenicSpot> getById(@PathVariable Long id) {
        return Result.success(scenicSpotService.getById(id));
    }

    /**
     * 新增景区
     */
    @PostMapping
    public Result<Boolean> add(@RequestBody ScenicSpot scenicSpot) {
        return Result.success(scenicSpotService.save(scenicSpot));
    }

    /**
     * 更新景区信息
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody ScenicSpot scenicSpot) {
        return Result.success(scenicSpotService.updateById(scenicSpot));
    }

    /**
     * 删除景区
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(scenicSpotService.removeById(id));
    }
}
