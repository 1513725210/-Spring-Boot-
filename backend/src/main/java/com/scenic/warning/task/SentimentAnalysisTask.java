package com.scenic.warning.task;

import com.scenic.warning.entity.ScenicSpot;
import com.scenic.warning.service.ScenicSpotService;
import com.scenic.warning.service.WarningLogService;
import com.scenic.warning.websocket.WarningWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
public class SentimentAnalysisTask {

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
    private final String PYTHON_AI_URL = "http://127.0.0.1:5000/api/ai/sentiment_analysis";

    @Autowired
    private ScenicSpotService scenicSpotService;

    @Autowired
    private WarningLogService warningLogService;

    private final Random random = new Random();

    // 模拟的游客评价语料库
    private static final List<String> REVIEWS_CORPUS = Arrays.asList(
            "风景很美，空气很好！",
            "人太多了，挤死了，体验很差！",
            "排队两小时，看景五分钟，能不能退票啊？",
            "还行吧，一般般。",
            "有人插队还打架，安保人员在哪里？！",
            "带小孩来的，虽然人多但是秩序还可以。",
            "完全走不动，不要来！避雷避雷！",
            "门票太贵了，不划算。"
    );

    /**
     * 每 15 分钟执行一次舆情分析
     * (在开发环境下，为了演示效果，改为每 2 分钟执行一次)
     */
    @Scheduled(fixedRate = 120000)
    public void analyzeSentiment() {
        log.info("--- Starting regular Sentiment Analysis Task ---");
        
        // 随机挑选几个状态为开放的重点景区进行分析
        List<ScenicSpot> spots = scenicSpotService.lambdaQuery().eq(ScenicSpot::getStatus, 1).last("LIMIT 3").list();
        if (spots.isEmpty()) return;

        for (ScenicSpot spot : spots) {
            String simulatedReviews = generateSimulatedReviews();
            log.info("Analyzing reviews for {}: {}", spot.getName(), simulatedReviews);

            String prompt = "你是景区舆情分析专家。我将给你几条针对某景区的最新游客评论。\n" +
                    "请进行情感分析，判断是否出现【严重的多人聚集、退票、打架、挤死了】等负面情绪。\n" +
                    "只需要回答：【正常】或者【舆情预警】，并简要说明提取到的关键标签（如：挤死了、退票）。\n" +
                    "不要输出多余解释。";

            try {
                Map<String, Object> request = new HashMap<>();
                request.put("reviews", simulatedReviews);
                
                org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                        PYTHON_AI_URL, request, Map.class);
                        
                String aiAnalysis = "正常";
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Boolean success = (Boolean) response.getBody().get("success");
                    if (success != null && success) {
                        aiAnalysis = (String) response.getBody().get("analysis");
                    }
                }

                log.info("AI Analysis result: {}", aiAnalysis);

                if (aiAnalysis.contains("舆情预警")) {
                    log.warn("🚨 AI detected negative sentiment for {}. Triggering Service Warning!", spot.getName());
                    
                    // 记录一个特殊的黄色或者舆情预警
                    warningLogService.logWarning(
                            spot.getId(),
                            spot.getName(),
                            "YELLOW", // 舆情暂定为黄警
                            spot.getCurrentCount(),
                            spot.getMaxCapacity(),
                            0.0, // 阈值无关
                            (double)spot.getCurrentCount() / spot.getMaxCapacity() * 100
                    );

                    // 通过 WebSocket推送特殊消息
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("type", "WARNING");
                    msg.put("level", "YELLOW");
                    msg.put("scenicName", spot.getName());
                    msg.put("scenicId", spot.getId());
                    msg.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    msg.put("message", "🔔【舆情预警】" + aiAnalysis);
                    msg.put("congestionRate", "N/A");
                    msg.put("currentCount", spot.getCurrentCount());

                    WarningWebSocket.broadcastMessage(msg);
                }
            } catch (Exception e) {
                log.error("Sentiment analysis failed for {}", spot.getName(), e);
            }
        }
    }

    private String generateSimulatedReviews() {
        StringBuilder sb = new StringBuilder();
        // 随机挑 3-5 条来模拟该景区的当前评论
        int count = 3 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            sb.append(REVIEWS_CORPUS.get(random.nextInt(REVIEWS_CORPUS.size()))).append("\n");
        }
        return sb.toString();
    }
}
