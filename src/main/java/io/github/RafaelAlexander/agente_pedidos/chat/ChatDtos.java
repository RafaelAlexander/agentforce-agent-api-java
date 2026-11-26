package io.github.RafaelAlexander.agente_pedidos.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Our own API contract, independent of the Agent API shapes. */
public final class ChatDtos {

    private ChatDtos() {
    }

    /** What the client sends. Validated: it's untrusted input that ends up in an LLM prompt. */
    public record MessageRequest(@NotBlank @Size(max = 2000) String text) {
    }

    public record SessionStarted(String sessionId, String greeting) {
    }

    public record ChatReply(String reply) {
    }
}
