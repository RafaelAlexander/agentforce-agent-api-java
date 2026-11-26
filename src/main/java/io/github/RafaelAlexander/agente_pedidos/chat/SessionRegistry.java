package io.github.RafaelAlexander.agente_pedidos.chat;

import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Remembers which app user owns each agent session, and the message sequence number.
 *
 * Why it matters: an Agent API session carries the customer's identity. If bob could send
 * messages to alice's sessionId, he would talk to the agent AS alice. That's an IDOR in our
 * own app, even though the Salesforce side is secure. So every call checks ownership.
 *
 * In memory: fine for one instance. With several instances, use Redis or a database.
 */
@Component
public class SessionRegistry {

    private record Entry(String owner, AtomicLong sequence) {
    }

    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();

    public void register(String sessionId, String owner) {
        sessions.put(sessionId, new Entry(owner, new AtomicLong()));
    }

    /** Next sequenceId for this session, or empty if it doesn't exist or isn't the user's. */
    public OptionalLong nextSequence(String sessionId, String owner) {
        Entry entry = sessions.get(sessionId);
        if (entry == null || !entry.owner().equals(owner)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(entry.sequence().incrementAndGet());
    }

    /** Removes the session only if it belongs to the user. */
    public boolean remove(String sessionId, String owner) {
        Entry entry = sessions.get(sessionId);
        if (entry == null || !entry.owner().equals(owner)) {
            return false;
        }
        return sessions.remove(sessionId, entry);
    }
}
