package com.example.shopagent.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class IntentClassifierTest {
    private final IntentClassifier classifier = new IntentClassifier(null);

    @Test
    void ruleBasedDetectsComplaint() {
        assertThat(classifier.classifyByRule("我要投诉客服")).isEqualTo(Intent.COMPLAINT);
        assertThat(classifier.classifyByRule("我要举报")).isEqualTo(Intent.COMPLAINT);
    }

    @Test
    void ruleBasedDetectsActionByKeywords() {
        assertThat(classifier.classifyByRule("帮我查订单 O1001")).isEqualTo(Intent.ACTION);
        assertThat(classifier.classifyByRule("申请退款")).isEqualTo(Intent.ACTION);
        assertThat(classifier.classifyByRule("我的物流到哪了")).isEqualTo(Intent.ACTION);
        assertThat(classifier.classifyByRule("有什么优惠券")).isEqualTo(Intent.ACTION);
    }

    @Test
    void ruleBasedDetectsInquiryByKeywords() {
        // Regression: "退货是什么意思" should be INQUIRY, not COMPLAINT or ACTION
        assertThat(classifier.classifyByRule("7天无理由退货是什么意思")).isEqualTo(Intent.INQUIRY);
        assertThat(classifier.classifyByRule("退款流程是怎样的")).isEqualTo(Intent.INQUIRY);
        assertThat(classifier.classifyByRule("几天能到货")).isEqualTo(Intent.INQUIRY);
        assertThat(classifier.classifyByRule("运费怎么算")).isEqualTo(Intent.INQUIRY);
    }

    @Test
    void inquiryWithActionWordsStillInquiry() {
        // "查订单怎么查" is asking HOW to check — still INQUIRY
        assertThat(classifier.classifyByRule("查订单怎么查")).isEqualTo(Intent.INQUIRY);
        // "帮我查订单 O1001" has action verb "帮我" → ACTION
        assertThat(classifier.classifyByRule("帮我查订单 O1001")).isEqualTo(Intent.ACTION);
    }

    @Test
    void ruleBasedReturnsNullForAmbiguous() {
        assertThat(classifier.classifyByRule("你好啊")).isNull();
        assertThat(classifier.classifyByRule("这个东西不错")).isNull();
    }

    @Test
    void blacklistedUserForceComplaint() {
        assertThat(classifier.classifyWithUserContext("哪里都行", 999L)).isEqualTo(Intent.COMPLAINT);
    }
}
