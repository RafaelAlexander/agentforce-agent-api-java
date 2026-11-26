package io.github.RafaelAlexander.agente_pedidos.agentapi;

import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.AgentResponse;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.InstanceConfig;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.Message;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.SendMessageRequest;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.StartSessionRequest;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.StreamingCapabilities;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.Variable;
import io.github.RafaelAlexander.agente_pedidos.config.AgentforceProperties;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Thin client for the Agentforce Agent API: start session, send message, end session. */
@Component
public class AgentApiClient {

    /** Context variable the agent's action binds to (with contactId = @variables.ContactId). */
    private static final String CONTACT_VARIABLE = "$Context.ContactId";

    private final AgentforceProperties props;
    private final TokenProvider tokens;
    private final RestClient restClient;

    public AgentApiClient(AgentforceProperties props, TokenProvider tokens) {
        this.props = props;
        this.tokens = tokens;
        this.restClient = RestClient.builder().baseUrl(props.apiBaseUrl()).build();
    }

    /**
     * Starts a session for a verified customer. Context variables can only be set here
     * and can't be changed later in the conversation, so the identity stays fixed.
     */
    public AgentResponse startSession(String contactId) {
        var body = new StartSessionRequest(
                UUID.randomUUID().toString(),
                new InstanceConfig(props.domain()),
                new StreamingCapabilities(List.of("Text")),
                true,
                List.of(new Variable(CONTACT_VARIABLE, "Text", contactId)));

        return withToken(token -> restClient.post()
                .uri("/agents/{agentId}/sessions", props.agentId())
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AgentResponse.class));
    }

    public AgentResponse sendMessage(String sessionId, long sequenceId, String text) {
        var body = new SendMessageRequest(new Message(sequenceId, "Text", text));

        return withToken(token -> restClient.post()
                .uri("/sessions/{sessionId}/messages", sessionId)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AgentResponse.class));
    }

    public void endSession(String sessionId) {
        withToken(token -> restClient.delete()
                .uri("/sessions/{sessionId}", sessionId)
                .headers(h -> {
                    h.setBearerAuth(token);
                    h.set("x-session-end-reason", "UserRequest");
                })
                .retrieve()
                .toBodilessEntity());
    }

    /** Runs the call with the cached token; on 401, refreshes the token once and retries. */
    private <T> T withToken(Function<String, T> call) {
        try {
            return call.apply(tokens.getToken());
        } catch (HttpClientErrorException.Unauthorized e) {
            tokens.invalidate();
            return call.apply(tokens.getToken());
        }
    }
}
