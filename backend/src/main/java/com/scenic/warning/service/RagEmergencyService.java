package com.scenic.warning.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class RagEmergencyService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String PYTHON_AI_URL = "http://127.0.0.1:5000/api/ai/generate_plan";

    private String sopContextCache;

    @PostConstruct
    public void initSopContext() {
        try {
            ClassPathResource resource = new ClassPathResource("emergency_sop.txt");
            sopContextCache = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            log.info("Loaded emergency SOP into memory ({} chars)", sopContextCache.length());
        } catch (Exception e) {
            log.error("Failed to load emergency_sop.txt", e);
            sopContextCache = "无法加载应急预案知识库。";
        }
    }

    /**
     * 根据实时警报情况生成定制化应急处置方案
     */
    public String generateEmergencyPlan(String scenicName, String level, int currentCount, double congestionRate) {
        String prompt = "你是景区安全指挥中心的高级决策助手。当景区发生拥挤预警时，你的任务是根据预警级别和现场情况，" +
                "结合给定的《应急预案知识库(SOP)》，生成一份条理清晰、可以直接执行的3-4步应急指令，提供给现场管理人员。\n\n" +
                "【知识库内容】:\n" + sopContextCache + "\n\n" +
                "请根据以上知识库，为以下紧急情况生成处置指南，要求使用 HTML 格式（方便前端渲染），包含 `<ul>` 和 `<li>`。" +
                "不要输出其他的寒暄语等。";

        String userQuery = String.format("当前发生预警！\n地点: %s\n预警级别: %s (红色为100%%满载，黄色为80%%较高)\n当前人数: %d\n拥挤度: %.2f%%\n" +
                        "请立即生成针对该情况的定向处置步骤。",
                scenicName,
                level.equals("RED") ? "红色预警(严重拥挤)" : "黄色预警(客流高峰)",
                currentCount,
                congestionRate);

        try {
            log.info("Step 3 (RAG): Generating emergency plan via Python AI Service...");
            
            java.util.Map<String, Object> request = new java.util.HashMap<>();
            request.put("scenic_name", scenicName);
            request.put("level", level);
            request.put("current_count", currentCount);
            request.put("congestion_rate", congestionRate);
            request.put("sop_context", sopContextCache);

            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                    PYTHON_AI_URL, request, java.util.Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                if (success != null && success) {
                    return (String) response.getBody().get("plan");
                }
            }
            return "<ul><li>预案生成服务返回异常。</li></ul>";
        } catch (Exception e) {
            log.error("RAG Plan generation failed", e);
            return "<ul><li>自动生成应急预案失败，请立即启动线下人工指挥程序！</li></ul>";
        }
    }
}
