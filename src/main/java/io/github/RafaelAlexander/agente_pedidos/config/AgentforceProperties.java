package io.github.RafaelAlexander.agente_pedidos.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Connection settings for Salesforce and the Agent API.
 * Values come from environment variables (see application.properties), never from code.
 * If any is missing, the app fails at startup instead of failing on the first request.
 */
@Validated
@ConfigurationProperties("agentforce")
public record AgentforceProperties(
        @NotBlank String domain,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String agentId,
        @NotBlank String apiBaseUrl) {
}
