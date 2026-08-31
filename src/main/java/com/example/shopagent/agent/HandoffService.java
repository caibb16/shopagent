package com.example.shopagent.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class HandoffService {
    private final com.example.shopagent.tool.EscalateTool escalateTool;

    public Flux<String> handoff(long userId, String sessionId, String userText) {
        return Flux.just("正在为您转接人工客服，请稍候...\n")
                .concatWith(Flux.defer(() -> {
                    try {
                        com.example.shopagent.tool.UserContext.set(
                                new com.example.shopagent.tool.UserContext(userId, sessionId));
                        // null ToolContext → UserContext.resolveUserId falls back to
                        // the ThreadLocal this method sets. The HandoffService runs
                        // the tool synchronously on the same worker thread, so the
                        // ThreadLocal is safe here (it's the ToolAgent / streaming
                        // path where cross-thread propagation was needed).
                        var r = escalateTool.escalateToHuman(userText, "用户触发投诉/升级", null);
                        return Flux.just("已为您排队，当前等待 " + r.get("queuePosition") + " 人，预计等待 " + r.get("eta"));
                    } finally {
                        com.example.shopagent.tool.UserContext.clear();
                    }
                }));
    }
}
