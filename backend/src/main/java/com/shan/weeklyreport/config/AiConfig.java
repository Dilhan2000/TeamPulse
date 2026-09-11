package com.shan.weeklyreport.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI Chat Assistant and Anthropic Claude Java SDK (SRS08 C8-T01).
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Value("${app.ai.enabled:false}")
    private boolean enabled;

    @Value("${app.ai.model:claude-3-5-sonnet-20241022}")
    private String model;

    @Value("${app.ai.max-tokens:2048}")
    private long maxTokens;

    @Value("${app.ai.max-tool-iterations:5}")
    private int maxToolIterations;

    public boolean isEnabled() {
        return enabled;
    }

    public String getModel() {
        return model;
    }

    public long getMaxTokens() {
        return maxTokens;
    }

    public int getMaxToolIterations() {
        return maxToolIterations;
    }

    @Bean
    @ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
    public AnthropicClient anthropicClient() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("app.ai.enabled=true but ANTHROPIC_API_KEY environment variable is not set. AnthropicClient cannot be initialized.");
            return null;
        }
        log.info("Initializing Anthropic Claude client with model: {}", model);
        return AnthropicOkHttpClient.fromEnv();
    }
}
