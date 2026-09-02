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
 * requires an API key at boot. The OpenAiApi / OpenAiChatModel chain is assembled manually
 * here so that the bean constructs eagerly without validating the key; only a real call
 * would fail.
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
     * Shared worker pool available for any blocking subscriber in the SSE chat
     * pipeline. As of the toolContext refactor, the ChatController no longer
     * pins the stream onto this pool (Reactor/Spring AI handle thread
     * hops internally and tool identity is propagated via {@code ToolContext}).
     * The bean is retained for any future blocking consumer and to satisfy
     * the existing {@code ChatControllerE2ETest} wiring.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService chatWorkerPool() {
        return Executors.newFixedThreadPool(16);
    }
}
