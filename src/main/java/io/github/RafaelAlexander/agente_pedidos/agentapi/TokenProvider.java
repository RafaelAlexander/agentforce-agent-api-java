package io.github.RafaelAlexander.agente_pedidos.agentapi;

import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.TokenResponse;
import io.github.RafaelAlexander.agente_pedidos.config.AgentforceProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Gets a JWT access token with the OAuth client credentials flow and caches it.
 * The External Client App must have "Issue JWT-based access tokens" enabled:
 * the Agent API rejects the classic "00D..." tokens.
 */
@Component
public class TokenProvider {

    private static final Duration CACHE_TTL = Duration.ofMinutes(20);

    private final AgentforceProperties props;
    private final RestClient restClient;

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public TokenProvider(AgentforceProperties props) {
        this.props = props;
        this.restClient = RestClient.builder().baseUrl(props.domain()).build();
    }

    public synchronized String getToken() {
        if (token == null || Instant.now().isAfter(expiresAt)) {
            token = fetchToken();
            expiresAt = Instant.now().plus(CACHE_TTL);
        }
        return token;
    }

    /** Called when the Agent API answers 401, so the next call gets a fresh token. */
    public synchronized void invalidate() {
        token = null;
    }

    private String fetchToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());

        TokenResponse response = restClient.post()
                .uri("/services/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Salesforce did not return an access token");
        }
        return response.accessToken();
    }
}
