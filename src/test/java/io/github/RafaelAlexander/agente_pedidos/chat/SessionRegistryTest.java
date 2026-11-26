package io.github.RafaelAlexander.agente_pedidos.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionRegistryTest {

    private SessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SessionRegistry();
        registry.register("s1", "alice");
    }

    @Test
    void ownerGetsIncreasingSequenceIds() {
        assertThat(registry.nextSequence("s1", "alice")).hasValue(1);
        assertThat(registry.nextSequence("s1", "alice")).hasValue(2);
        assertThat(registry.nextSequence("s1", "alice")).hasValue(3);
    }

    @Test
    void anotherUserCannotUseTheSession() {
        // The IDOR case in our own app: bob knows alice's sessionId.
        assertThat(registry.nextSequence("s1", "bob")).isEmpty();
    }

    @Test
    void anotherUserDoesNotConsumeTheOwnersSequence() {
        registry.nextSequence("s1", "bob");
        assertThat(registry.nextSequence("s1", "alice")).hasValue(1);
    }

    @Test
    void unknownSessionIsEmpty() {
        assertThat(registry.nextSequence("does-not-exist", "alice")).isEmpty();
    }

    @Test
    void onlyTheOwnerCanRemoveTheSession() {
        assertThat(registry.remove("s1", "bob")).isFalse();
        assertThat(registry.nextSequence("s1", "alice")).hasValue(1); // still alive

        assertThat(registry.remove("s1", "alice")).isTrue();
        assertThat(registry.nextSequence("s1", "alice")).isEmpty();  // gone
    }

    @Test
    void removingTwiceReturnsFalseTheSecondTime() {
        assertThat(registry.remove("s1", "alice")).isTrue();
        assertThat(registry.remove("s1", "alice")).isFalse();
    }
}
