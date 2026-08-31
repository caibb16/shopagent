package com.example.shopagent.api;

import com.example.shopagent.agent.AgentService;
import com.example.shopagent.api.error.SseErrorSender;
import com.example.shopagent.session.Message;
import com.example.shopagent.session.SessionStore;
import com.example.shopagent.session.SessionSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebFlux SSE chat endpoint. The project uses spring-boot-starter-webflux
 * (no MVC on classpath), so we return {@code Flux<ServerSentEvent<String>>}
 * instead of Spring MVC's blocking SseEmitter.
 *
 * <p>Identity propagation: per-request userId/sessionId are passed through
 * the agent pipeline via ChatClient's {@code .toolContext(Map)} so every
 * tool method receives a {@code ToolContext} containing the userId on
 * whatever thread Spring AI uses for tool execution. A ThreadLocal
 * UserContext was previously set here, but Spring AI tool callbacks run on
 * separate threads (Netty / worker pool / tool-callback thread) where the
 * ThreadLocal was invisible — see {@code UserContext.resolveUserId} for the
 * ToolContext-first resolver.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AgentService agentService;
    private final SessionStore sessionStore;
    private final SessionSummaryService summaryService;
    private final SseErrorSender errorSender;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestHeader("X-User-Id") Long userId) {

        sessionStore.appendMessage(sessionId, Message.user(message));

        StringBuilder full = new StringBuilder();

        Flux<ServerSentEvent<String>> stream = agentService.handle(userId, sessionId, message)
                .doOnNext(full::append)
                .map(SseErrorSender::dataEvent);

        return stream
                .concatWith(Mono.fromRunnable(() -> {
                    sessionStore.appendMessage(sessionId, Message.assistant(full.toString()));
                    summaryService.maybeSummarize(sessionId,
                            sessionStore.getHistory(sessionId, 1000).size());
                }).thenReturn(SseErrorSender.namedEvent("done", "")))
                .onErrorResume(err -> {
                    log.warn("stream error", err);
                    return Flux.just(errorSender.toEvent(err));
                });
    }
}
