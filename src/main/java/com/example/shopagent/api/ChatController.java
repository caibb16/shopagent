package com.example.shopagent.api;

import com.example.shopagent.agent.AgentService;
import com.example.shopagent.api.error.SseErrorSender;
import com.example.shopagent.session.Message;
import com.example.shopagent.session.SessionStore;
import com.example.shopagent.session.SessionSummaryService;
import com.example.shopagent.tool.UserContext;
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
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutorService;

/**
 * WebFlux SSE chat endpoint. The project uses spring-boot-starter-webflux
 * (no MVC on classpath), so we return {@code Flux<ServerSentEvent<String>>}
 * instead of Spring MVC's blocking SseEmitter. All streaming + tool-callback
 * work runs on the shared {@code chatWorkerPool} so the ThreadLocal
 * {@link UserContext} stays valid for the entire stream lifecycle.
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
    private final ExecutorService chatWorkerPool;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestHeader("X-User-Id") Long userId) {

        sessionStore.appendMessage(sessionId, Message.user(message));

        // Flux.defer + subscribeOn routes the entire pipeline (including the
        // agentService.handle(...) subscription and tool callbacks) onto a worker
        // thread from chatWorkerPool. UserContext is set at the start of defer so
        // every downstream operator + tool invocation sees it via the same thread.
        return Flux.defer(() -> {
            UserContext.set(new UserContext(userId, sessionId));
            StringBuilder full = new StringBuilder();

            Flux<ServerSentEvent<String>> stream = agentService.handle(userId, sessionId, message)
                    .doOnNext(full::append)
                    .map(SseErrorSender::dataEvent);

            Flux<ServerSentEvent<String>> finalized = stream.concatWith(Mono.fromRunnable(() -> {
                sessionStore.appendMessage(sessionId, Message.assistant(full.toString()));
                summaryService.maybeSummarize(sessionId,
                        sessionStore.getHistory(sessionId, 1000).size());
            }).thenReturn(SseErrorSender.namedEvent("done", "")));

            return finalized
                    .onErrorResume(err -> {
                        log.warn("stream error", err);
                        return Flux.just(errorSender.toEvent(err));
                    })
                    .doFinally(sig -> UserContext.clear());
        })
        .subscribeOn(Schedulers.fromExecutor(chatWorkerPool));
    }
}