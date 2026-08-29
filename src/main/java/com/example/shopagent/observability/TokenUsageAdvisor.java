package com.example.shopagent.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Accumulates prompt/completion token counts reported by the model across the
 * lifetime of the application. Runs after {@link LoggingAdvisor} (order 1) so
 * usage is tallied once the response has been logged.
 */
@Slf4j
public class TokenUsageAdvisor implements CallAroundAdvisor {

    private final AtomicLong totalPromptTokens = new AtomicLong();
    private final AtomicLong totalCompletionTokens = new AtomicLong();

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        AdvisedResponse resp = chain.nextAroundCall(request);
        record(resp);
        return resp;
    }

    private void record(AdvisedResponse resp) {
        if (resp == null || resp.response() == null) {
            return;
        }
        var meta = resp.response().getMetadata();
        if (meta == null || meta.getUsage() == null) {
            return;
        }
        var usage = meta.getUsage();
        // Spring AI 1.0.0-M6 exposes these as Integer and they may be null.
        long p = usage.getPromptTokens() == null ? 0L : usage.getPromptTokens();
        long c = usage.getCompletionTokens() == null ? 0L : usage.getCompletionTokens();
        totalPromptTokens.addAndGet(p);
        totalCompletionTokens.addAndGet(c);
        log.info("[Tokens] prompt={}, completion={}, totalPrompt={}, totalCompletion={}",
                p, c, totalPromptTokens.get(), totalCompletionTokens.get());
    }

    public long getTotalPromptTokens() {
        return totalPromptTokens.get();
    }

    public long getTotalCompletionTokens() {
        return totalCompletionTokens.get();
    }

    @Override
    public String getName() {
        return "TokenUsageAdvisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
