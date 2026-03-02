package com.scenic.warning.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DataStewardService {

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
    private final String PYTHON_AI_URL_SQL = "http://127.0.0.1:5000/api/ai/sql_generation";
    private final String PYTHON_AI_URL_SUMMARY = "http://127.0.0.1:5000/api/ai/data_summary";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 数据库 Schema 的精简提示词，帮助大模型生成准确的 SQL
    private static final String SCHEMA_PROMPT = 
        "你是一个景区数据管家（AI Agent），你的任务是将用户的自然语言查询转换为 MySQL 查询语句（Text-to-SQL），" +
        "并基于查询结果回答用户问题。\n" +
        "\n" +
        "【数据库表结构说明】\n" +
        "1. 表 `scenic_spot` (景区信息表)\n" +
        "   字段: id(主键), name(景区名称), city(城市), max_capacity(最大承载量), current_count(当前人数), level(等级), status(状态 1开放 0关闭)\n" +
        "2. 表 `flow_record` (客流历史流水表)\n" +
        "   字段: id, scenic_id(景区ID), current_count(记录时的客流人数), in_count(进入人数), out_count(离开人数), congestion_rate(拥挤度%), record_time(记录时间)\n" +
        "3. 表 `warning_log` (预警记录表)\n" +
        "   字段: id, scenic_id, scenic_name(景区名), warning_level(YELLOW/RED), current_count, max_capacity, threshold_percent, congestion_rate, message, warning_time\n" +
        "\n" +
        "【交互规则】\n" +
        "你需要输出合法的 SQL 语句来查询数据，必须用 ```sql 和 ``` 包裹 SQL 语句。\n" +
        "只能包含一条 SELECT 查询，绝对禁止 DELETE/UPDATE/INSERT 等修改操作。\n" +
        "如果用户的问题需要查询数据库，请只返回 ```sql...```，不需要解释说明。";

    /**
     * 处理用户自然语言查询的主流程
     */
    public String processUserQuery(String userQuery) {
        // 第一步：Text-to-SQL
        log.info("Step 1: Asking AI to generate SQL for query: {}", userQuery);
        String sqlGenerationResponse = "";
        try {
            java.util.Map<String, Object> request = new java.util.HashMap<>();
            request.put("query", userQuery);
            request.put("schema_prompt", SCHEMA_PROMPT);
            
            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                    PYTHON_AI_URL_SQL, request, java.util.Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                if (success != null && success) {
                    sqlGenerationResponse = (String) response.getBody().get("sql");
                }
            }
        } catch (Exception e) {
            log.error("Error generating SQL via Python AI service", e);
        }
        
        String sql = sqlGenerationResponse;
        if (sql == null || sql.trim().isEmpty() || !sql.toLowerCase().contains("select")) {
            log.warn("AI failed to generate viable SQL: {}", sqlGenerationResponse);
            return "我很抱歉，未能理解您的查询意图。请使用更清晰的自然语言询问景区客流等相关数据。";
        }

        // 安全检查 (非常重要)
        if (sql.toLowerCase().matches(".*(delete|update|insert|drop|truncate|alter).*")) {
            throw new SecurityException("非法且危险的 SQL 语句被拒绝运行！");
        }

        // 第二步：执行 SQL 获取数据
        log.info("Step 2: Executing generated SQL: {}", sql);
        List<Map<String, Object>> dbResult;
        try {
            dbResult = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("SQL 执行失败", e);
            // 将错误交给 AI 分析（可选），这里直接简单重试或者提示
            return "我很抱歉，在尝试从数据库获取信息时遇到了一点问题。错误信息：" + e.getMessage();
        }

        // 将结果转为字符串 (简单截断防止上下文超限)
        String dataStr = dbResult.toString();
        if (dataStr.length() > 2000) {
            dataStr = dataStr.substring(0, 2000) + "...(数据已截断)";
        }

        // 第三步：Data-to-Text 自然语言分析与总结
        log.info("Step 3: Asking AI to summarize the result");
        
        try {
            java.util.Map<String, Object> request = new java.util.HashMap<>();
            request.put("query", userQuery);
            request.put("db_result", dataStr);
            
            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                    PYTHON_AI_URL_SUMMARY, request, java.util.Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                if (success != null && success) {
                    return (String) response.getBody().get("summary");
                }
            }
        } catch (Exception e) {
            log.error("Error summarizing data via Python AI service", e);
        }
        
        return "查询成功，但生成自然语言报告失败。原始数据：" + dataStr;
    }

    /**
     * 使用正则从 Markdown 中提取 SQL
     */
    private String extractSql(String response) {
        Pattern pattern = Pattern.compile("```sql(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 如果没有 markdown 包裹，判断是否本身就是简单的 SELECT 语句
        if (response.trim().toUpperCase().startsWith("SELECT")) {
            return response.trim();
        }
        return null; // 无法提取
    }
}
