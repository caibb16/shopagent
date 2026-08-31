package com.example.shopagent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class EscalateTool {

    @Tool(description = "将会话升级到人工客服。需要用户已登录；用于记录当前用户被升级到人工队列的请求。")
    public Map<String, Object> escalateToHuman(
            @ToolParam(description = "用户反馈的问题") String issue,
            @ToolParam(description = "问题摘要") String summary,
            ToolContext toolContext) {
        log.warn("[ESCALATION] user={}, issue={}, summary={}",
                UserContext.resolveUserId(toolContext), issue, summary);
        // prod: enqueue to RabbitMQ for human-agent queue
        return Map.of("status", "QUEUED", "queuePosition", 3, "eta", "2 minutes");
    }
}
