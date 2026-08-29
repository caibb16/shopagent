package com.example.shopagent.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.shopagent.rag.PromptTemplates;

import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClassifier {
    private static final Set<String> COMPLAINT_WORDS = Set.of("投诉", "举报", "差评", "退钱", "黑名单", "曝光");
    private static final Set<String> ACTION_WORDS = Set.of("订单", "物流", "快递", "退款", "退换", "优惠券", "推荐", "查一下", "查我的");
    private static final Pattern ORDER_ID = Pattern.compile("O\\d{3,}");
    private final ChatClient chatClient;

    public Intent classify(String userText) {
        return classifyWithUserContext(userText, null);
    }

    public Intent classifyWithUserContext(String userText, Long userId) {
        Intent byRule = classifyByRule(userText);
        if (byRule != null) return byRule;
        // blacklisted users always handled by human
        if (userId != null && userId == 999L) return Intent.COMPLAINT;
        // fallback to LLM
        try {
            String raw = chatClient.prompt()
                    .user(PromptTemplates.intentClassifier(userText))
                    .call().content();
            return Intent.fromRaw(raw);
        } catch (Exception e) {
            log.warn("LLM intent classification failed, defaulting to CHITCHAT", e);
            return Intent.CHITCHAT;
        }
    }

    Intent classifyByRule(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        for (String w : COMPLAINT_WORDS) if (lower.contains(w)) return Intent.COMPLAINT;
        if (ORDER_ID.matcher(text).find()) return Intent.ACTION;
        for (String w : ACTION_WORDS) if (lower.contains(w)) return Intent.ACTION;
        return null;
    }
}
