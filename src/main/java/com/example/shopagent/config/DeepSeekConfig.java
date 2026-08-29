package com.example.shopagent.config;

import com.example.shopagent.observability.LoggingAdvisor;
import com.example.shopagent.observability.TokenUsageAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Builds the {@link ChatClient} used by every Agent.
 *
 * <p>Spring AI's {@code OpenAiAutoConfiguration} is excluded in application.yml because it
 * hard-requires an API key at boot, which would break clone-and-run in the dev profile. So
 * the OpenAiApi / OpenAiChatModel chain is assembled manually here — this constructs
 * eagerly without validating the key, and only a real call would fail.
 */
@Configuration
public class DeepSeekConfig {

    @Value("${shopagent.llm.api-key}")
    private String apiKey;

    @Value("${shopagent.llm.base-url}")
    private String baseUrl;

    @Value("${shopagent.llm.model}")
    private String model;

    @Bean
    public ChatClient chatClient() {
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.3)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(new LoggingAdvisor(), new TokenUsageAdvisor())
                .build();
    }

    /**
     * Shared worker pool for the SSE chat endpoint. The HTTP request thread returns
     * immediately with the SseEmitter; the actual AgentService.handle(...) subscription
     * (including tool callbacks) runs here. A ThreadLocal UserContext survives on this
     * thread for the entire stream lifecycle.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService chatWorkerPool() {
        return Executors.newFixedThreadPool(16);
    }
}
