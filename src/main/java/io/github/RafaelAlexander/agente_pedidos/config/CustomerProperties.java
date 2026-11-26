package io.github.RafaelAlexander.agente_pedidos.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Maps an authenticated app user to their Salesforce Contact Id.
 * Demo only: in a real app this link lives in your user database or identity provider.
 */
@ConfigurationProperties("app")
public record CustomerProperties(Map<String, String> customers) {

    public CustomerProperties {
        customers = customers == null ? Map.of() : Map.copyOf(customers);
    }
}
