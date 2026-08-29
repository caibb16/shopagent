package com.example.shopagent.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PromptTemplatesTest {
    @Test
    void customerServiceSubstitutesBothPlaceholders() {
        String out = PromptTemplates.customerService("KB", "SUMMARY");
        assertThat(out).contains("KB").contains("SUMMARY");
        assertThat(out).doesNotContain("%s");
    }

    @Test
    void intentClassifierSubstitutesUserText() {
        String out = PromptTemplates.intentClassifier("查订单");
        assertThat(out).contains("查订单");
    }

    @Test
    void queryRewriterSubstitutesUserText() {
        String out = PromptTemplates.queryRewriter("多久能到货");
        assertThat(out).contains("多久能到货");
    }
}
