package io.github.RafaelAlexander.agente_pedidos.chat;

import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiClient;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.AgentMessage;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.AgentResponse;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatDtos.ChatReply;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatDtos.SessionStarted;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatExceptions.CustomerNotLinkedException;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatExceptions.SessionNotFoundException;
import io.github.RafaelAlexander.agente_pedidos.config.CustomerProperties;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final AgentApiClient agent;
    private final SessionRegistry registry;
    private final CustomerProperties customers;

    public ChatService(AgentApiClient agent, SessionRegistry registry, CustomerProperties customers) {
        this.agent = agent;
        this.registry = registry;
        this.customers = customers;
    }

    /** The contactId comes from the authenticated user, never from the client's request. */
    public SessionStarted start(String username) {
        String contactId = customers.customers().get(username);
        if (contactId == null) {
            throw new CustomerNotLinkedException(username);
        }
        AgentResponse response = agent.startSession(contactId);
        registry.register(response.sessionId(), username);
        return new SessionStarted(response.sessionId(), joinMessages(response));
    }

    public ChatReply send(String username, String sessionId, String text) {
        long sequenceId = registry.nextSequence(sessionId, username)
                .orElseThrow(SessionNotFoundException::new);
        return new ChatReply(joinMessages(agent.sendMessage(sessionId, sequenceId, text)));
    }

    public void end(String username, String sessionId) {
        if (!registry.remove(sessionId, username)) {
            throw new SessionNotFoundException();
        }
        agent.endSession(sessionId);
    }

    private static String joinMessages(AgentResponse response) {
        if (response == null || response.messages() == null) {
            return "";
        }
        return response.messages().stream()
                .map(AgentMessage::message)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }
}
