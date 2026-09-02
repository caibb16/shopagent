package com.example.shopagent.api;

import com.example.shopagent.agent.AgentService;
import com.example.shopagent.api.error.SseErrorSender;
import com.example.shopagent.session.InMemorySessionStore;
import com.example.shopagent.session.SessionSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Slice test for {@link ChatController} hitting the real
 * {@code GET /api/chat/stream} endpoint. Downstream collaborators
 * (AgentService, SessionSummaryService) are mocked; InMemorySessionStore,
 * SseErrorSender, and a single-thread worker pool are wired in as real
 * beans so the controller pipeline (including {@code subscribeOn}) actually
 * runs.
 */
@WebFluxTest(controllers = ChatController.class)
@Import({InMemorySessionStore.class, SseErrorSender.class, ChatControllerE2ETest.TestBeans.class})
class ChatControllerE2ETest {

    @Autowired
    WebTestClient client;

    @MockBean
    AgentService agentService;

    @MockBean
    SessionSummaryService summaryService;

    @Test
    void streamReturnsChunks() {
        when(agentService.handle(anyLong(), anyString(), anyString()))
                .thenReturn(Flux.just("你好", "，", "小蜜"));

        client.get().uri(uri -> uri.path("/api/chat/stream")
                        .queryParam("sessionId", "s1")
                        .queryParam("message", "你好")
                        .build())
                .header("X-User-Id", "1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    // SSE frames look like: data: <chunk>\n\n
                    assertThat(body).contains("你好").contains("小蜜");
                });
    }

    /**
     * The controller subscribes its SSE pipeline onto {@code chatWorkerPool}
     * via {@code Schedulers.fromExecutor(...)}. A mock ExecutorService
     * silently no-ops every submitted task, which would hang the Flux.
     * A real single-thread executor is enough to drive the subscription.
     */
    @TestConfiguration
    static class TestBeans {
        @Bean(destroyMethod = "shutdown")
        @Primary
        ExecutorService chatWorkerPool() {
            return Executors.newSingleThreadExecutor();
        }
    }
}