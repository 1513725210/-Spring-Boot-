package com.scenic.warning.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scenic.warning.config.DeepSeekProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeepSeekClient {

    @Autowired
    private DeepSeekProperties properties;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送聊天完成请求到 DeepSeek
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return 模型的回复文本
     */
    public String chatCompletion(String systemPrompt, String userMessage) {
        return chatCompletion(systemPrompt, userMessage, 0.7);
    }
    
    /**
     * 发送聊天完成请求到 DeepSeek (指定 temperature)
     */
    public String chatCompletion(String systemPrompt, String userMessage, double temperature) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        // stream: false by default

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Sending request to DeepSeek API...");
            ResponseEntity<String> response = restTemplate.postForEntity(
                    properties.getBaseUrl(),
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    return message.getString("content");
                }
            } else {
                log.error("DeepSeek API error: {} - {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Exception calling DeepSeek API", e);
        }

        return "很抱歉，AI 思考过程中出现了问题，请稍后重试。";
    }
}
