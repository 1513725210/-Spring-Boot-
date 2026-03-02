package com.scenic.warning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
public class DeepSeekProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
}
