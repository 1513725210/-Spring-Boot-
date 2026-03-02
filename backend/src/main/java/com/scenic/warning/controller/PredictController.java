package com.scenic.warning.controller;

import com.scenic.warning.common.Result;
import com.scenic.warning.entity.FlowRecord;
import com.scenic.warning.service.FlowRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ARIMA 预测接口 Controller
 * 通过调用 Python 脚本实现客流趋势预测
 */
@Slf4j
@RestController
@RequestMapping("/api/predict")
public class PredictController {

    @Resource
    private FlowRecordService flowRecordService;

    @Value("${arima.python-path:python}")
    private String pythonPath;

    @Value("${arima.script-path:../python/arima_predict.py}")
    private String scriptPath;

    /**
     * 调用 ARIMA 模型进行客流预测
     *
     * @param scenicId 景区ID
     * @param steps    预测步数（每步代表一个时间间隔）
     */
    @GetMapping("/{scenicId}")
    public Result<Map<String, Object>> predict(
            @PathVariable Long scenicId,
            @RequestParam(defaultValue = "12") Integer steps) {
        try {
            // 获取最近的客流数据
            List<FlowRecord> records = flowRecordService.getLatestRecords(scenicId, 200);

            if (records.size() < 20) {
                return Result.error("历史数据不足，至少需要20条记录才能进行预测");
            }

            // 反转为时间正序
            List<FlowRecord> sorted = new ArrayList<>(records);
            java.util.Collections.reverse(sorted);

            // 提取客流数值序列
            String dataStr = sorted.stream()
                    .map(r -> String.valueOf(r.getCurrentCount()))
                    .collect(Collectors.joining(","));

            // 调用 Python ARIMA 脚本
            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath, scriptPath, dataStr, String.valueOf(steps));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            String lastLine = "";
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (!line.trim().isEmpty()) {
                    lastLine = line.trim();
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("ARIMA 脚本执行失败, exitCode={}, output={}", exitCode, output);
                return Result.error("预测模型执行失败: " + output.toString());
            }

            // 解析 Python 输出 (JSON 格式)
            String resultStr = lastLine;
            com.alibaba.fastjson2.JSONObject jsonResult =
                    com.alibaba.fastjson2.JSON.parseObject(resultStr);

            Map<String, Object> result = new HashMap<>();
            result.put("scenicId", scenicId);
            result.put("predictions", jsonResult.getJSONArray("predictions"));
            result.put("confidence_lower", jsonResult.getJSONArray("confidence_lower"));
            result.put("confidence_upper", jsonResult.getJSONArray("confidence_upper"));
            result.put("steps", steps);

            return Result.success(result);

        } catch (Exception e) {
            log.error("ARIMA 预测异常", e);
            return Result.error("预测失败: " + e.getMessage());
        }
    }
}
