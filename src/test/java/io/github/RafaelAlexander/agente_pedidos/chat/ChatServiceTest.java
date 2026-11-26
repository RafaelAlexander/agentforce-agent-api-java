package io.github.RafaelAlexander.agente_pedidos.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiClient;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.AgentMessage;
import io.github.RafaelAlexander.agente_pedidos.agentapi.AgentApiDtos.AgentResponse;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatDtos.ChatReply;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatDtos.SessionStarted;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatExceptions.CustomerNotLinkedException;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatExceptions.SessionNotFoundException;
import io.github.RafaelAlexander.agente_pedidos.config.CustomerProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ChatService. The Agent API is mocked: these tests never call Salesforce,
 * so they run in milliseconds and check OUR logic (identity and session ownership).
 */
class ChatServiceTest {

    private static final String ALICE_CONTACT = "003000000000001";

    private AgentApiClient agent;
    private ChatService chat;

    @BeforeEach
    void setUp() {
        agent = mock(AgentApiClient.class);
        var customers = new CustomerProperties(Map.of("alice", ALICE_CONTACT));
        chat = new ChatService(agent, new SessionRegistry(), customers);

        when(agent.startSession(anyString()))
                .thenReturn(response("s1", "Hi, I'm an AI assistant."));
        when(agent.sendMessage(anyString(), anyLong(), anyString()))
                .thenReturn(response("s1", "Order 100 status: Draft: not confirmed yet."));
    }

    // ---------- Identity ----------

    @Test
    void startSendsTheAuthenticatedUsersContactId() {
        SessionStarted started = chat.start("alice");

        verify(agent).startSession(ALICE_CONTACT);
        assertThat(started.sessionId()).isEqualTo("s1");
        assertThat(started.greeting()).isEqualTo("Hi, I'm an AI assistant.");
    }

    @Test
    void userWithoutLinkedCustomerCannotStartAndTheAgentIsNeverCalled() {
        assertThatThrownBy(() -> chat.start("mallory"))
                .isInstanceOf(CustomerNotLinkedException.class);

        verify(agent, never()).startSession(any());
    }

    // ---------- Session ownership ----------

    @Test
    void ownerCanSendMessages() {
        chat.start("alice");

        ChatReply reply = chat.send("alice", "s1", "What is the status of order 100?");

        assertThat(reply.reply()).contains("Draft");
    }

    @Test
    void anotherUserCannotSendToTheSessionAndTheAgentIsNeverCalled() {
        chat.start("alice");

        assertThatThrownBy(() -> chat.send("bob", "s1", "What is the status of order 100?"))
                .isInstanceOf(SessionNotFoundException.class);

        verify(agent, never()).sendMessage(anyString(), anyLong(), anyString());
    }

    @Test
    void unknownSessionIsNotFound() {
        assertThatThrownBy(() -> chat.send("alice", "nope", "hi"))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void sequenceIdsIncreaseWithEachMessage() {
        chat.start("alice");

        chat.send("alice", "s1", "first");
        chat.send("alice", "s1", "second");

        verify(agent).sendMessage("s1", 1L, "first");
        verify(agent).sendMessage("s1", 2L, "second");
    }

    @Test
    void anotherUserCannotEndTheSession() {
        chat.start("alice");

        assertThatThrownBy(() -> chat.end("bob", "s1"))
                .isInstanceOf(SessionNotFoundException.class);

        verify(agent, never()).endSession(any());
    }

    @Test
    void ownerEndsTheSessionAndItCanNoLongerBeUsed() {
        chat.start("alice");

        chat.end("alice", "s1");

        verify(agent).endSession("s1");
        assertThatThrownBy(() -> chat.send("alice", "s1", "hi"))
                .isInstanceOf(SessionNotFoundException.class);
    }

    // ---------- Response mapping ----------

    @Test
    void multipleAgentMessagesAreJoined() {
        when(agent.sendMessage(eq("s1"), anyLong(), anyString())).thenReturn(new AgentResponse("s1", List.of(
                new AgentMessage("Inform", "Order 100 status: Draft."),
                new AgentMessage("Inform", "Anything else?"))));
        chat.start("alice");

        ChatReply reply = chat.send("alice", "s1", "status?");

        assertThat(reply.reply()).isEqualTo("Order 100 status: Draft.\nAnything else?");
    }

    @Test
    void emptyAgentResponseBecomesEmptyReply() {
        when(agent.sendMessage(eq("s1"), anyLong(), anyString())).thenReturn(new AgentResponse("s1", null));
        chat.start("alice");

        assertThat(chat.send("alice", "s1", "status?").reply()).isEmpty();
    }

    private static AgentResponse response(String sessionId, String text) {
        return new AgentResponse(sessionId, List.of(new AgentMessage("Inform", text)));
    }
}
