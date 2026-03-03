package com.scenic.warning.scheduler;

import com.scenic.warning.entity.FlowRecord;
import com.scenic.warning.entity.ScenicSpot;
import com.scenic.warning.entity.ThresholdConfig;
import com.scenic.warning.service.FlowRecordService;
import com.scenic.warning.service.ScenicSpotService;
import com.scenic.warning.service.ThresholdConfigService;
import com.scenic.warning.service.WarningLogService;
import com.scenic.warning.websocket.WarningWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 客流模拟器
 *
 * 使用正弦波形 + 随机扰动模拟真实景区客流变化：
 * - 早高峰 (9:00-11:00): 客流快速上升
 * - 午间峰值 (11:00-14:00): 维持较高水平
 * - 晚高峰 (14:00-16:00): 缓慢下降
 * - 傍晚 (16:00-18:00): 快速下降
 * - 夜间 (18:00-08:00): 极少客流
 */
@Slf4j
@Component
public class FlowSimulator {

    @Resource
    private ScenicSpotService scenicSpotService;

    @Resource
    private FlowRecordService flowRecordService;

    @Resource
    private ThresholdConfigService thresholdConfigService;

    @Resource
    private WarningLogService warningLogService;

    @Value("${simulator.enabled:true}")
    private boolean enabled;

    private final Random random = new Random();

    /**
     * 每5秒模拟一次客流数据
     */
    @Scheduled(fixedDelayString = "${simulator.interval-ms:5000}")
    public void simulateFlow() {
        if (!enabled) {
            return;
        }

        List<ScenicSpot> spots = scenicSpotService.getActiveSpots();
        if (spots.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> allData = new ArrayList<>();

        for (ScenicSpot spot : spots) {
            // 校验最大承载量，防止除零异常
            Integer maxCap = spot.getMaxCapacity();
            if (maxCap == null || maxCap <= 0) {
                log.warn("【数据异常】景区 '{}' (id={}) 的最大承载量为 {} ，已使用默认值 10000 进行计算",
                        spot.getName(), spot.getId(), maxCap);
                maxCap = 10000;
                spot.setMaxCapacity(maxCap);
            }

            // 计算模拟客流
            int simulatedCount = calculateSimulatedCount(spot, now);

            // 更新景区当前人数
            scenicSpotService.updateCurrentCount(spot.getId(), simulatedCount);

            // 计算拥挤度（保留两位小数）
            double congestionRate = BigDecimal.valueOf((double) simulatedCount / maxCap * 100)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();

            // 写入客流记录
            FlowRecord record = new FlowRecord();
            record.setScenicId(spot.getId());
            record.setCurrentCount(simulatedCount);
            record.setCongestionRate(BigDecimal.valueOf(congestionRate).setScale(2, RoundingMode.HALF_UP));
            record.setRecordTime(now);
            record.setSource("SIMULATOR");
            flowRecordService.save(record);

            // 预警检测
            checkWarning(spot, simulatedCount, congestionRate);

            // 构建推送数据
            Map<String, Object> data = new HashMap<>();
            data.put("scenicId", spot.getId());
            data.put("scenicName", spot.getName());
            data.put("currentCount", simulatedCount);
            data.put("maxCapacity", spot.getMaxCapacity());
            data.put("congestionRate", BigDecimal.valueOf(congestionRate).setScale(2, RoundingMode.HALF_UP));
            data.put("longitude", spot.getLongitude());
            data.put("latitude", spot.getLatitude());
            data.put("recordTime", now.toString());
            allData.add(data);
        }

        // 通过 WebSocket 广播实时数据
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("type", "FLOW_UPDATE");
        wsMessage.put("data", allData);
        wsMessage.put("timestamp", now.toString());

        String flowJson = com.alibaba.fastjson2.JSON.toJSONString(wsMessage);
        log.info("【WebSocket推送-客流】即将广播 FLOW_UPDATE，数据条数={}，JSON={}", allData.size(), flowJson);
        WarningWebSocket.broadcastMessage(wsMessage);

        log.debug("客流模拟完成，更新了 {} 个景区的数据", spots.size());
    }

    /**
     * 计算模拟客流量
     *
     * 利用正弦波形模拟一天内的客流变化规律：
     * - 基础波形：以中午12点为峰值的正弦曲线
     * - 叠加随机扰动：±15% 的随机波动
     * - 不同景区有不同的"热度系数"
     */
    private int calculateSimulatedCount(ScenicSpot spot, LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        int hour = time.getHour();
        int minute = time.getMinute();

        // 将时间转换为0-24的小数
        double timeDecimal = hour + minute / 60.0;

        // 基础客流比例（0.0 ~ 1.0）
        double baseRatio;

        if (timeDecimal >= 7 && timeDecimal < 9) {
            // 早间上升期 (7:00-9:00): 从低到中等
            baseRatio = 0.1 + 0.4 * (timeDecimal - 7) / 2.0;
        } else if (timeDecimal >= 9 && timeDecimal < 11) {
            // 上午高峰 (9:00-11:00): 从中等到高峰
            baseRatio = 0.5 + 0.35 * (timeDecimal - 9) / 2.0;
        } else if (timeDecimal >= 11 && timeDecimal < 14) {
            // 午间维持高峰 (11:00-14:00): 使用正弦波在峰值附近波动
            double peakWave = Math.sin((timeDecimal - 11) / 3.0 * Math.PI);
            baseRatio = 0.75 + 0.15 * peakWave;
        } else if (timeDecimal >= 14 && timeDecimal < 16) {
            // 下午缓降 (14:00-16:00)
            baseRatio = 0.85 - 0.25 * (timeDecimal - 14) / 2.0;
        } else if (timeDecimal >= 16 && timeDecimal < 18) {
            // 傍晚快速下降 (16:00-18:00)
            baseRatio = 0.6 - 0.45 * (timeDecimal - 16) / 2.0;
        } else if (timeDecimal >= 18 && timeDecimal < 20) {
            // 傍晚到夜间 (18:00-20:00)
            baseRatio = 0.15 - 0.1 * (timeDecimal - 18) / 2.0;
        } else {
            // 夜间和凌晨 (20:00-7:00): 极低
            baseRatio = 0.02 + 0.03 * random.nextDouble();
        }

        // 景区热度系数（根据景区等级和承载量调整）
        double heatFactor = getHeatFactor(spot);

        // 添加随机扰动 (±15%)
        double noise = 1.0 + (random.nextGaussian() * 0.1);
        noise = Math.max(0.85, Math.min(1.15, noise));

        // 最终客流 = 最大承载量 × 基础比例 × 热度系数 × 随机扰动
        int count = (int) (spot.getMaxCapacity() * baseRatio * heatFactor * noise);

        // 确保在合理范围内
        return Math.max(0, Math.min(count, (int) (spot.getMaxCapacity() * 1.2)));
    }

    /**
     * 景区热度系数
     */
    private double getHeatFactor(ScenicSpot spot) {
        // 5A景区更热门
        if ("5A".equals(spot.getLevel())) {
            return 0.7 + random.nextDouble() * 0.3;
        } else if ("4A".equals(spot.getLevel())) {
            return 0.5 + random.nextDouble() * 0.3;
        } else {
            return 0.3 + random.nextDouble() * 0.3;
        }
    }

    /**
     * 预警检测
     */
    private void checkWarning(ScenicSpot spot, int currentCount, double congestionRate) {
        ThresholdConfig config = thresholdConfigService.getByScenicId(spot.getId());

        if (config == null || config.getEnableWarning() == 0) {
            return;
        }

        double yellowThreshold = config.getYellowPercent().doubleValue();
        double redThreshold = config.getRedPercent().doubleValue();

        String warningLevel = null;

        if (congestionRate >= redThreshold) {
            warningLevel = "RED";
        } else if (congestionRate >= yellowThreshold) {
            warningLevel = "YELLOW";
        }

        if (warningLevel != null) {
            // 记录预警日志
            warningLogService.logWarning(
                    spot.getId(), spot.getName(), warningLevel,
                    currentCount, spot.getMaxCapacity(),
                    "RED".equals(warningLevel) ? redThreshold : yellowThreshold,
                    congestionRate
            );

            // 通过 WebSocket 推送预警
            Map<String, Object> warning = new HashMap<>();
            warning.put("type", "WARNING");
            warning.put("level", warningLevel);
            warning.put("scenicId", spot.getId());
            warning.put("scenicName", spot.getName());
            warning.put("currentCount", currentCount);
            warning.put("maxCapacity", spot.getMaxCapacity());
            warning.put("congestionRate", BigDecimal.valueOf(congestionRate).setScale(2, RoundingMode.HALF_UP));
            warning.put("threshold", "RED".equals(warningLevel) ? redThreshold : yellowThreshold);
            warning.put("timestamp", LocalDateTime.now().toString());

            String warningJson = com.alibaba.fastjson2.JSON.toJSONString(warning);
            log.info("【WebSocket推送-预警】即将广播 WARNING，景区={}，级别={}，拥挤度={}%，JSON={}",
                    spot.getName(), warningLevel, BigDecimal.valueOf(congestionRate).setScale(2, RoundingMode.HALF_UP), warningJson);
            WarningWebSocket.broadcastMessage(warning);

            log.warn("【{}预警】{} 当前客流{}人，拥挤度{:.1f}%",
                    warningLevel, spot.getName(), currentCount, congestionRate);
        }
    }
}
