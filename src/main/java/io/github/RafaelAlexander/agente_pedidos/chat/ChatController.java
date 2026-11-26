package io.github.RafaelAlexander.agente_pedidos.chat;

import io.github.RafaelAlexander.agente_pedidos.chat.ChatDtos.ChatReply;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatDtos.MessageRequest;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatDtos.SessionStarted;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/sessions")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionStarted start(Principal principal) {
        return chat.start(principal.getName());
    }

    @PostMapping("/{sessionId}/messages")
    public ChatReply send(Principal principal,
                          @PathVariable String sessionId,
                          @Valid @RequestBody MessageRequest request) {
        return chat.send(principal.getName(), sessionId, request.text());
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void end(Principal principal, @PathVariable String sessionId) {
        chat.end(principal.getName(), sessionId);
    }
}
