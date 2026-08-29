package com.example.shopagent.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

public final class PromptTemplates {
    private PromptTemplates() {}

    private static String load(String name) {
        try (var in = new ClassPathResource("prompts/" + name).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt: " + name, e);
        }
    }

    public static String customerService(String kbContext, String summary) {
        return String.format(load("customer-service.st"), kbContext, summary);
    }

    public static String intentClassifier(String userText) {
        return String.format(load("intent-classifier.st"), userText);
    }

    public static String queryRewriter(String userText) {
        return String.format(load("query-rewriter.st"), userText);
    }

    public static String summary(String priorSummary, String transcript) {
        return String.format(load("summary.st"), priorSummary, transcript);
    }
}
