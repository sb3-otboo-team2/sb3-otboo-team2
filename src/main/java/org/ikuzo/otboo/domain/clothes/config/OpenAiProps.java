package org.ikuzo.otboo.domain.clothes.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "openai")
public record OpenAiProps(
    @NotBlank String baseUrl,
    @NotBlank String apiKey,
    @NotBlank String model,
    @NotNull Integer timeoutMs
) {

    @Override
    public String toString() {
        return "OpenAiProps[baseUrl=%s, model=%s, timeoutMs=%s, apiKey=****]"
            .formatted(baseUrl, model, timeoutMs);
    }
}
