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
    // Positive signals for INQUIRY (policy/info questions, not actions)
    private static final Set<String> INQUIRY_WORDS = Set.of(
            "是什么", "什么意思", "怎么", "如何", "几天", "多少天", "能不能", "可以吗",
            "政策", "规则", "流程", "说明", "介绍", "解释", "区别", "条件");
    // Explicit action verbs — only these + action nouns = ACTION
    private static final Set<String> ACTION_VERBS = Set.of(
            "帮我", "查一下", "查我的", "申请", "给我", "我要", "我想", "帮我查", "帮我退");
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
        // INQUIRY keywords checked BEFORE ACTION — "退货是什么意思" is a question, not an action
        boolean hasInquiry = false;
        for (String w : INQUIRY_WORDS) { if (lower.contains(w)) { hasInquiry = true; break; } }
        if (hasInquiry) {
            // Only override to ACTION if there's an explicit action verb (e.g. "帮我查订单怎么查")
            boolean hasActionVerb = false;
            for (String w : ACTION_VERBS) { if (lower.contains(w)) { hasActionVerb = true; break; } }
            if (!ORDER_ID.matcher(text).find() && !hasActionVerb) return Intent.INQUIRY;
        }
        if (ORDER_ID.matcher(text).find()) return Intent.ACTION;
        for (String w : ACTION_WORDS) if (lower.contains(w)) return Intent.ACTION;
        return null;
    }
}
