package org.ikuzo.otboo.domain.clothes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProps(String baseUrl, String apiKey, String model, Integer timeoutMs) {}
