package io.github.RafaelAlexander.agente_pedidos.chat;

import io.github.RafaelAlexander.agente_pedidos.chat.ChatExceptions.CustomerNotLinkedException;
import io.github.RafaelAlexander.agente_pedidos.chat.ChatExceptions.SessionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** Maps errors to RFC 9457 "problem details". Never leaks Salesforce error bodies to the client. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(SessionNotFoundException.class)
    public ProblemDetail sessionNotFound(SessionNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CustomerNotLinkedException.class)
    public ProblemDetail customerNotLinked(CustomerNotLinkedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** The Agent API (or the token endpoint) answered with an error. */
    @ExceptionHandler(RestClientResponseException.class)
    public ProblemDetail agentError(RestClientResponseException e) {
        log.error("Agent API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "The agent service returned an error");
    }

    /** Salesforce couldn't be reached (network, DNS, timeout). */
    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail agentUnreachable(ResourceAccessException e) {
        log.error("Agent API unreachable", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "The agent service is unavailable");
    }
}
