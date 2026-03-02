package com.scenic.warning.controller;

import com.scenic.warning.common.Result;
import com.scenic.warning.entity.FlowRecord;
import com.scenic.warning.entity.ScenicSpot;
import com.scenic.warning.entity.ThresholdConfig;
import com.scenic.warning.service.FlowRecordService;
import com.scenic.warning.service.ScenicSpotService;
import com.scenic.warning.service.ThresholdConfigService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 大屏数据接口 Controller
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    private ScenicSpotService scenicSpotService;

    @Resource
    private FlowRecordService flowRecordService;

    @Resource
    private ThresholdConfigService thresholdConfigService;

    /**
     * 获取大屏总览数据
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        List<ScenicSpot> spots = scenicSpotService.getActiveSpots();

        int totalSpots = spots.size();
        int totalCurrentCount = spots.stream().mapToInt(ScenicSpot::getCurrentCount).sum();
        int totalCapacity = spots.stream().mapToInt(ScenicSpot::getMaxCapacity).sum();
        int warningCount = 0;

        List<Map<String, Object>> spotDataList = new ArrayList<>();

        for (ScenicSpot spot : spots) {
            ThresholdConfig config = thresholdConfigService.getByScenicId(spot.getId());
            double congestionRate = spot.getMaxCapacity() > 0
                    ? (double) spot.getCurrentCount() / spot.getMaxCapacity() * 100 : 0;

            String status = "NORMAL";
            if (config != null) {
                if (congestionRate >= config.getRedPercent().doubleValue()) {
                    status = "RED";
                    warningCount++;
                } else if (congestionRate >= config.getYellowPercent().doubleValue()) {
                    status = "YELLOW";
                    warningCount++;
                }
            }

            Map<String, Object> spotData = new HashMap<>();
            spotData.put("id", spot.getId());
            spotData.put("name", spot.getName());
            spotData.put("city", spot.getCity());
            spotData.put("longitude", spot.getLongitude());
            spotData.put("latitude", spot.getLatitude());
            spotData.put("currentCount", spot.getCurrentCount());
            spotData.put("maxCapacity", spot.getMaxCapacity());
            spotData.put("congestionRate", BigDecimal.valueOf(congestionRate).setScale(2, RoundingMode.HALF_UP));
            spotData.put("status", status);
            spotData.put("level", spot.getLevel());
            spotDataList.add(spotData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalSpots", totalSpots);
        result.put("totalCurrentCount", totalCurrentCount);
        result.put("totalCapacity", totalCapacity);
        result.put("warningCount", warningCount);
        result.put("spots", spotDataList);

        return Result.success(result);
    }

    /**
     * 获取指定景区的客流趋势数据（最近24小时）
     */
    @GetMapping("/trend/{scenicId}")
    public Result<List<FlowRecord>> getTrend(@PathVariable Long scenicId) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(24);
        List<FlowRecord> records = flowRecordService.getByTimeRange(scenicId, start, end);
        return Result.success(records);
    }

    /**
     * 获取所有景区的实时拥挤度排行
     */
    @GetMapping("/ranking")
    public Result<List<Map<String, Object>>> getRanking() {
        List<ScenicSpot> spots = scenicSpotService.getActiveSpots();
        List<Map<String, Object>> ranking = new ArrayList<>();

        for (ScenicSpot spot : spots) {
            double congestionRate = spot.getMaxCapacity() > 0
                    ? (double) spot.getCurrentCount() / spot.getMaxCapacity() * 100 : 0;

            Map<String, Object> item = new HashMap<>();
            item.put("id", spot.getId());
            item.put("name", spot.getName());
            item.put("currentCount", spot.getCurrentCount());
            item.put("maxCapacity", spot.getMaxCapacity());
            item.put("congestionRate", BigDecimal.valueOf(congestionRate).setScale(2, RoundingMode.HALF_UP));
            ranking.add(item);
        }

        // 按拥挤度降序排列
        ranking.sort((a, b) -> ((BigDecimal) b.get("congestionRate"))
                .compareTo((BigDecimal) a.get("congestionRate")));

        return Result.success(ranking);
    }
}
