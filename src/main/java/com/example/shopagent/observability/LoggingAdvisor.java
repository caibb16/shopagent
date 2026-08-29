package com.example.shopagent.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;

/**
 * Logs the outbound prompt text and the inbound completion text around every
 * blocking LLM call. Runs first in the advisor chain (order 0) so the request is
 * logged before any other advisor mutates it.
 */
@Slf4j
public class LoggingAdvisor implements CallAroundAdvisor {

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        log.debug("[LLM] -> {}", request.userText());
        AdvisedResponse resp = chain.nextAroundCall(request);
        log.debug("[LLM] <- {}", extractText(resp));
        return resp;
    }

    private String extractText(AdvisedResponse resp) {
        if (resp == null || resp.response() == null || resp.response().getResult() == null) {
            return "<no response>";
        }
        var output = resp.response().getResult().getOutput();
        return output == null ? "<no output>" : output.getText();
    }

    @Override
    public String getName() {
        return "LoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
