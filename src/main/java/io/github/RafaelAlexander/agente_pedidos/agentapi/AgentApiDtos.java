package io.github.RafaelAlexander.agente_pedidos.agentapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Request/response shapes of the Salesforce token endpoint and the Agent API. */
public final class AgentApiDtos {

    private AgentApiDtos() {
    }

    // ---------- OAuth ----------
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    // ---------- Start session ----------
    public record StartSessionRequest(
            String externalSessionKey,
            InstanceConfig instanceConfig,
            StreamingCapabilities streamingCapabilities,
            boolean bypassUser,
            List<Variable> variables) {
    }

    public record InstanceConfig(String endpoint) {
    }

    public record StreamingCapabilities(List<String> chunkTypes) {
    }

    /** Agent variable. Context variables use the "$Context." prefix, e.g. "$Context.ContactId". */
    public record Variable(String name, String type, String value) {
    }

    // ---------- Send message ----------
    public record SendMessageRequest(Message message) {
    }

    public record Message(long sequenceId, String type, String text) {
    }

    // ---------- Responses ----------
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentResponse(String sessionId, List<AgentMessage> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentMessage(String type, String message) {
    }
}
