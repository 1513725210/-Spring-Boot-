package com.scenic.warning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scenic.warning.common.Result;
import com.scenic.warning.entity.WarningLog;
import com.scenic.warning.service.WarningLogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预警日志 Controller
 */
@RestController
@RequestMapping("/api/warning")
public class WarningLogController {

    @Resource
    private WarningLogService warningLogService;

    /**
     * 分页查询预警日志
     */
    @GetMapping("/page")
    public Result<Page<WarningLog>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long scenicId,
            @RequestParam(required = false) Integer handled) {
        Page<WarningLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WarningLog> wrapper = new LambdaQueryWrapper<>();

        if (level != null && !level.isEmpty()) {
            wrapper.eq(WarningLog::getWarningLevel, level);
        }
        if (scenicId != null) {
            wrapper.eq(WarningLog::getScenicId, scenicId);
        }
        if (handled != null) {
            wrapper.eq(WarningLog::getHandled, handled);
        }

        wrapper.orderByDesc(WarningLog::getWarningTime);
        return Result.success(warningLogService.page(page, wrapper));
    }

    /**
     * 获取最近的预警记录
     */
    @GetMapping("/recent")
    public Result<List<WarningLog>> getRecent(@RequestParam(defaultValue = "20") Integer limit) {
        LambdaQueryWrapper<WarningLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WarningLog::getWarningTime).last("LIMIT " + limit);
        return Result.success(warningLogService.list(wrapper));
    }

    /**
     * 处理预警
     */
    @PutMapping("/handle/{id}")
    public Result<Boolean> handle(@PathVariable Long id,
                                   @RequestParam String handleUser,
                                   @RequestParam(required = false) String remark) {
        WarningLog log = warningLogService.getById(id);
        if (log == null) {
            return Result.error("预警记录不存在");
        }
        log.setHandled(1);
        log.setHandleUser(handleUser);
        log.setHandleTime(LocalDateTime.now());
        log.setHandleRemark(remark);
        return Result.success(warningLogService.updateById(log));
    }
}
