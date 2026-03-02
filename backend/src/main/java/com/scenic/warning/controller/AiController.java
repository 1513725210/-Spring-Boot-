package com.scenic.warning.controller;

import com.scenic.warning.common.Result;
import com.scenic.warning.service.DataStewardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private DataStewardService dataStewardService;

    /**
     * 数据管家对话接口 (Text-to-SQL)
     * @param request 包含用户问题的 JSON {"query": "今天客流量如何？"}
     * @return AI 返回的分析报告
     */
    @PostMapping("/chat")
    public Result<String> chatWithDataSteward(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }
        
        try {
            String answer = dataStewardService.processUserQuery(query);
            return Result.success(answer);
        } catch (Exception e) {
            log.error("AI 数据管家处理失败", e);
            return Result.error("AI 思考出现异常：" + e.getMessage());
        }
    }
}
