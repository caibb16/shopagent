package com.example.shopagent.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class IntentClassifierTest {
    private final IntentClassifier classifier = new IntentClassifier(null);

    @Test
    void ruleBasedDetectsComplaint() {
        assertThat(classifier.classifyByRule("我要投诉客服")).isEqualTo(Intent.COMPLAINT);
    }

    @Test
    void ruleBasedDetectsActionByKeywords() {
        assertThat(classifier.classifyByRule("帮我查订单 O1001")).isEqualTo(Intent.ACTION);
        assertThat(classifier.classifyByRule("申请退款")).isEqualTo(Intent.ACTION);
        assertThat(classifier.classifyByRule("我的物流到哪了")).isEqualTo(Intent.ACTION);
        assertThat(classifier.classifyByRule("有什么优惠券")).isEqualTo(Intent.ACTION);
    }

    @Test
    void ruleBasedReturnsNullForAmbiguous() {
        assertThat(classifier.classifyByRule("7天无理由退货是什么意思？")).isNull();
        assertThat(classifier.classifyByRule("你好啊")).isNull();
    }

    @Test
    void blacklistedUserForceComplaint() {
        assertThat(classifier.classifyWithUserContext("哪里都行", 999L)).isEqualTo(Intent.COMPLAINT);
    }
}
