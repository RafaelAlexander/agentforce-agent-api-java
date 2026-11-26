package io.github.RafaelAlexander.agente_pedidos.chat;

public final class ChatExceptions {

    private ChatExceptions() {
    }

    /** The session doesn't exist OR belongs to another user: same answer for both. */
    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException() {
            super("Session not found");
        }
    }

    /** The authenticated user has no Salesforce customer linked. */
    public static class CustomerNotLinkedException extends RuntimeException {
        public CustomerNotLinkedException(String username) {
            super("User '" + username + "' is not linked to a customer");
        }
    }
}
