# 电商智能客服 Agent 设计文档

**目标**：为 Java 后端 / AI Agent 方向日常实习准备的简历项目，体现工程能力与独立思考。

**创建日期**：2026-08-28
**项目类型**：Spring Boot + Spring AI 实战项目
**业务场景**：电商客服（订单咨询、物流查询、退款工单、FAQ）
**复杂度目标**：2 周可交付 MVP，重点在"小而精、可深挖"

---

## 1. 项目背景与定位

### 1.1 为什么做这个项目
- **校招竞争现状**：Python + LangChain 做 AI 项目的人太多，Java + Spring AI 更差异化
- **岗位需求匹配**：Java 后端实习 + AI 应用能力是 2026 年大厂最稀缺的组合
- **可深挖性**：Function Calling / RAG / SSE / 工具鉴权 都是面试高频追问点

### 1.2 项目目标
- 体现 Spring Boot 全栈工程能力
- 体现 AI 应用架构设计能力
- 体现独立思考（不是教程搬运）
- 5 分钟能讲清楚架构，1-2 个模块能深挖 30 分钟

---

## 2. 技术选型

| 层 | 技术 | 选型理由 |
|----|------|---------|
| Web 框架 | Spring Boot 3.x + Spring WebFlux | SSE 流式响应原生支持 |
| AI 框架 | **Spring AI** | Java 生态官方 LLM 框架，2025 才成熟，差异化 |
| ORM | Spring Data JPA + MyBatis-Plus | MyBatis-Plus 写业务更顺手 |
| 缓存 | Redis + Redisson | 会话存储、分布式锁 |
| 消息队列 | RabbitMQ | 异步退款工单、订单状态事件 |
| 向量库 | **Qdrant**（推荐）或 Milvus | Qdrant 单文件部署，demo 友好 |
| LLM | DeepSeek API（OpenAI 兼容协议） | 性价比高、国内可访问 |
| Embedding | bge-small-zh / bge-large-zh-v1.5 | 中文友好 |
| 构建 | Maven | 主流 |
| 部署 | Docker + Docker Compose | 一键启动 |
| 监控 | Spring Boot Actuator + Langfuse（可选） | Token 消耗、调用链路 |

### 2.1 为什么选 Spring AI 而不是 LangChain4j
- Spring AI 是 Spring 官方生态，未来招聘市场更认可
- 与 Spring Boot 集成最丝滑（自动配置、starter）
- 文档质量在快速追赶 LangChain

---

## 3. 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│ Web 前端 (Vue3/React)                                       │
│              [对话窗口 + 商品/订单卡片渲染]                    │
└─────────────────────────────────────────────────────────────┘
                            ↓ SSE/WebSocket
┌─────────────────────────────────────────────────────────────┐
│ Spring Boot 后端                                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │ 对话API层 │→ │ Agent服务 │→ │ 工具调度器 │→ │ 业务查询 │     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
│       ↓           ↓             ↓             ↓             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                   │
│  │ 会话存储 │  │ RAG 检索 │  │ Prompt模板│                   │
│  │ (Redis)  │  │(向量DB)  │  │(可观测)  │                   │
│  └──────────┘  └──────────┘  └──────────┘                   │
└─────────────────────────────────────────────────────────────┘
                            ↓                ↓              ↓
                    MySQL           Milvus/Qdrant      DeepSeek
                  (订单/商品/用户)   (知识库)           (LLM API)
```

### 3.1 核心模块

| 模块 | 职责 | 涉及技术 |
|------|------|---------|
| 对话 API 层 | 接收用户消息、SSE 流式输出、会话管理 | Spring WebFlux + SSE |
| Agent 服务 | Spring AI ChatClient、Prompt 工程、意图路由 | Spring AI |
| 工具调度器 | Function Calling 注册、权限校验、参数解析 | Spring AI Tool |
| RAG 检索 | FAQ/政策知识库、向量化、TopK 召回、混合检索 | Spring AI Advisor + Qdrant |
| 业务查询 | 订单/物流/退款等底层接口（mock 即可） | Spring Data JPA + MySQL |

---

## 4. 核心流程

### 4.1 时序图

```
用户          对话API        Agent服务       工具调度器 RAG检索 LLM(DeepSeek)
 │              │              │              │              │              │
 │ 1.发送消息   │              │              │              │              │
 │─────────────→│              │              │              │              │
 │              │ 2.加载会话历史│              │              │              │
 │              │ (Redis)      │              │              │              │
 │              │─────────────→│              │              │              │
 │              │              │ 3.意图分类   │              │              │
 │              │              │─────────────────────────────→│ 闲聊/咨询/操作│
 │              │              │              │              │              │
 │              │              │ 4.检索知识(RAG,仅咨询类)     │              │
 │              │              │─────────────→│              │              │
 │              │              │←─topK文档 ───│              │              │
 │              │              │              │              │              │
 │              │              │ 5.组装Prompt + Tools         │              │
 │              │              │─────────────────────────────→│ 请求LLM      │
 │              │              │←─────────流式token──────────│              │
 │              │              │              │              │              │
 │              │              │ 6.若返回tool_calls:           │              │
 │              │              │─────────────→│              │              │
 │              │              │   鉴权→执行→返回结果         │              │
 │              │              │←─tool_result─│              │              │
 │              │              │              │              │              │
 │              │              │ 7.将tool结果再喂给LLM生成自然语言            │
 │              │              │─────────────────────────────→│              │
 │              │              │←─────────最终回复───────────│              │
 │              │ 8.SSE推送    │              │              │              │
 │←─────────────│              │              │              │              │
```

### 4.2 关键设计决策

#### 4.2.1 意图路由（避免一个 Agent 处理所有）
```java
public AgentResponse handle(Message msg) {
    Intent intent = classifyIntent(msg.text());  // 闲聊/咨询/操作/投诉
    return switch (intent) {
        case CHITCHAT -> simpleChatAgent.call(msg);
        case INQUIRY  -> ragAgent.call(msg, retrieveKB(msg));
        case ACTION   -> toolAgent.call(msg, allowedTools(user));
        case COMPLAINT -> handoffToHumanAgent(msg);  // 转人工
    };
}
```
**设计意图**：操作类请求走严格工具编排+鉴权，咨询类走 RAG，闲聊直接 LLM 答——避免"客服系统帮我退款"这种 prompt 注入风险。

#### 4.2.2 Function Calling 工具编排
工具设计原则：**窄而具体**，避免"超级工具"。

```java
@Tool(description = "根据订单号查询订单详情，需要用户登录态校验")
public OrderDetail getOrderDetail(@ToolArg("orderId") String orderId, @ToolArg("userId") Long userId) {
    Order order = orderRepo.findById(orderId)
        .orElseThrow(() -> new ToolAuthException("订单不存在"));
    if (!order.getUserId().equals(userId)) {
        throw new ToolAuthException("无权访问");
    }
    return order;
}
```

**注册的工具清单**（5-7 个就够）：
- `getOrderDetail(orderId, userId)` 订单详情
- `getLogistics(orderId)` 物流轨迹
- `createRefundOrder(orderId, reason)` 退款申请
- `getCouponList(userId)` 用户优惠券
- `recommendProducts(category, budget)` 商品推荐
- `escalateToHuman(issue, summary)` 转人工

#### 4.2.3 多轮对话与上下文压缩
```java
if (history.size() % 5 == 0) {
    String summary = chatClient.prompt()
        .system("请用100字以内总结以下对话的关键信息...")
        .user(history.toString())
        .call().content();
    redis.set("session:" + sid, summary);
}
```
**效果**：Token 消耗降低 60%，同时保留关键信息。

#### 4.2.4 流式响应（SSE）
```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@RequestParam String message) {
    SseEmitter emitter = new SseEmitter(60_000L);
    chatClient.stream(message).subscribe(
        chunk -> emitter.send(chunk),
        err -> emitter.completeWithError(err),
        () -> emitter.complete()
    );
    return emitter;
}
```

---

## 5. RAG 知识库设计

### 5.1 知识库内容

| 知识类型 | 数据来源 | 切片策略 |
|---------|---------|---------|
| 退换货政策 | 手动整理 50 条 FAQ | 按问题切，1 问 1 片 |
| 商品类目说明 | mock 数据 | 按品类切 |
| 活动规则 | 优惠券/促销文案 | 按活动 ID 切 |
| 售后流程 | 工单模板 | 按流程步骤切 |

### 5.2 检索增强设计

#### 5.2.1 混合检索（向量 + 关键词）
```java
@Component
public class HybridRetriever {
    public List<Document> retrieve(String query) {
        List<Document> vectorResults = qdrant.search(embed(query), topK=20);
        List<Document> keywordResults = esSearcher.search(query, topK=20);
        return reciprocalRankFusion(vectorResults, keywordResults);
    }
}
```
**收益**：政策类问题（"7天无理由"）关键词命中率高，口语化提问向量检索更好，两者融合 Recall 提升约 15%。

#### 5.2.2 查询改写（Query Rewrite）
- "多久能到货啊" → 改写为 "发货时效 配送时间"
- "怎么退货" → 改写为 "退货流程 退款政策"

#### 5.2.3 ReRank 重排序
初筛 top20 → Cross-Encoder ReRank 取 top3，提升精准度。

#### 5.2.4 元数据过滤
按时间过滤活动规则等。

### 5.3 离线评估体系

```
data/
  eval/
    test_set.jsonl    # 50-100 条标注数据
    evaluator.java    # 评估入口
```

**评估指标**：
- **召回率 Recall@K**：检索到的文档是否覆盖答案
- **答案准确率**：用 LLM-as-Judge 或人工标注
- **工具调用成功率**：Function Call 调对的比例

```java
@Component
public class RagEvaluator {
    public EvalReport evaluate(List<TestCase> cases) {
        int recallHit = 0, answerCorrect = 0;
        for (TestCase c : cases) {
            List<Document> retrieved = retriever.retrieve(c.query);
            if (containsAny(retrieved, c.expectedDocIds)) recallHit++;
            String answer = ragAgent.answer(c.query);
            if (judge(answer, c.expectedAnswer)) answerCorrect++;
        }
        return new EvalReport(recallHit * 1.0 / cases.size(),
                              answerCorrect * 1.0 / cases.size());
    }
}
```

### 5.4 Prompt 模板

```java
public static final String CUSTOMER_SERVICE_SYSTEM = """
你是「小蜜」，电商平台智能客服。你的职责：
1. 礼貌、简洁、专业
2. 仅基于【知识库上下文】和【工具结果】回答，不知道就说不知道
3. 操作类请求必须调用工具，不要凭空回答
4. 涉及退款/投诉，先安抚用户情绪再处理

【知识库上下文】
%s

【对话历史摘要】
%s
""";
```

---

## 6. 独立思考点（简历加分项）

| 设计决策 | 体现的能力 |
|---------|-----------|
| 意图分类后再选 Agent | 系统设计能力，避免 prompt 注入 |
| 工具鉴权（userId 校验） | 安全意识 |
| 历史摘要压缩 | 工程优化、成本意识 |
| 流式 SSE | 全栈能力 |
| 工具失败重试 + 降级 | 鲁棒性设计 |
| 混合检索（向量+BM25） | 不是简单调包 |
| 查询改写 | 理解检索增强 |
| 评估体系 | 工程化思维 |

---

## 7. 面试高频考点

### 7.1 后端基础（10 题）

| 考点 | 回答要点 |
|------|---------|
| Spring AI 怎么和 Spring Boot 集成？ | starter 依赖、ChatClient Bean 配置、Advisor 链 |
| Function Calling 的实现原理？ | LLM 返回结构化 JSON → Spring AI 解析 → 反射调用本地方法 → 把结果回填 LLM |
| 工具调用失败怎么降级？ | 重试 + 兜底话术 + 转人工 |
| SSE 和 WebSocket 区别？ | 单向、HTTP 兼容、断线自动重连，客服场景单向足够 |
| Redis 怎么存会话？ | `session:{userId}:{sessionId}`，Hash 存轮次 + JSON 存摘要 |
| 怎么控制 LLM 调用成本？ | 摘要压缩、缓存相似 query、限流 |
| Spring AI 的 Advisor 是什么？ | 类似 AOP，拦截请求做日志/鉴权/RAG 注入 |
| RabbitMQ 用在哪？ | 异步退款工单、订单状态变更触发主动通知客服 |
| MySQL 表怎么设计？ | user/order/refund/logistics 四张，加索引 |
| Docker Compose 一键启动？ | mysql + redis + qdrant + app 四个 service |

### 7.2 AI / Agent 专项（10 题）

| 考点 | 回答要点 |
|------|---------|
| Agent 和普通 LLM 调用区别？ | Agent 能自主规划、调用工具、多步推理 |
| ReAct 是什么？ | Reasoning + Acting 循环：Thought → Action → Observation |
| Function Calling vs Prompt 调用？ | FC 准确率更高；Prompt 调用灵活但不可靠 |
| RAG 检索不准怎么排查？ | 看召回 → 看 chunk 大小 → 看 embedding 模型 → 看 query 改写 |
| 向量库选型？ | Milvus 性能强，Qdrant 轻量单文件部署 |
| 怎么防止 prompt 注入？ | 系统 Prompt 约束、输入过滤、敏感操作二次确认 |
| 多 Agent 协作和单 Agent 区别？ | 单 Agent 适合窄场景，多 Agent 适合复杂任务分解 |
| Token 怎么计算？ | 中文约 1.5 字/token，英文 0.75 字/token |
| Embedding 模型选型？ | bge-small-zh 体积小中文好，bge-large 更准但慢 |
| 流式响应怎么实现？ | LLM SSE 接口 + Spring WebFlux SseEmitter 透传 |

---

## 8. 简历话术模板

**项目标题**：基于 Spring AI 的电商智能客服 Agent 平台

**项目描述**：
> 独立设计并实现基于 Spring AI 的电商智能客服 Agent，支持自然语言咨询、订单/物流查询、退款工单创建等场景。系统集成 Function Calling 多工具编排、RAG 知识库检索、多轮对话管理与 SSE 流式响应。

**技术栈**：
> Spring Boot 3 / Spring AI / Spring WebFlux / MySQL / Redis / RabbitMQ / Qdrant / Docker

**个人贡献**（STAR 5 条）：
1. **架构设计**：设计"意图路由 + 工具编排 + RAG"三层 Agent 架构，区分闲聊/咨询/操作/投诉四类场景，避免 prompt 注入风险
2. **工具鉴权**：实现 Function Calling 工具的用户级鉴权机制，确保 A 用户无法通过 prompt 越权访问 B 用户订单
3. **成本优化**：设计"摘要压缩 + 相似 query 缓存"机制，Token 消耗降低 60%，平均会话成本从 0.08 元降至 0.03 元
4. **检索增强**：实现向量检索 + BM25 关键词检索混合召回（RRF 融合），叠加查询改写与 ReRank，知识库召回率从 72% 提升至 91%
5. **评估体系**：构建 100 条标注测试集与离线评估 Pipeline，持续监控 RAG 召回率与 Agent 任务完成率

---

## 9. 实施时间表（2 周 MVP）

| 时间 | 内容 | 产出 |
|------|------|------|
| Day 1-2 | 环境搭建、Spring Boot + Spring AI 集成、调通 DeepSeek API | 能聊天的 demo |
| Day 3-4 | 业务表设计、mock 数据生成、4 个业务查询工具 | 后端可独立跑 |
| Day 5-6 | Function Calling 集成、工具鉴权、多轮对话 | Agent 可调用工具 |
| Day 7-8 | RAG 知识库搭建、Qdrant 集成、混合检索 | RAG 可检索 |
| Day 9-10 | SSE 流式响应、前端最小对话界面 | 端到端跑通 |
| Day 11-12 | 评估数据集、离线评估 Pipeline | 有量化指标 |
| Day 13-14 | Docker Compose 部署、README、简历话术打磨 | 可投递 |

---

## 10. 风险点与降级方案

| 风险 | 降级方案 |
|------|---------|
| Spring AI 国内文档少 | 看官方文档 + GitHub 源码，重点看 `ChatClient` 和 `Tool Calling` |
| LLM API 不稳定 | 加重试 + 熔断 + fallback 话术 |
| 评估数据集造数据难 | 用 GPT-4 生成 50 条种子数据 + 人工微调 |
| 前端耗时 | 用 V0 / Cursor 快速生成极简聊天界面 |

---

## 11. 最终交付物

- [ ] GitHub 仓库（完整代码 + README + 架构图）
- [ ] Docker Compose 一键启动
- [ ] Demo 视频 / 在线链接（可选）
- [ ] 简历项目描述（STAR 5 条）
- [ ] 面试 Q&A 准备（20 题）

---

## 12. 后续可演进方向（非 MVP 范围）

- 多 Agent 协作：商品推荐 Agent + 售后 Agent + 风控 Agent 联动
- 多模态：用户上传图片（破损/错发）识别 + 自动转接人工
- 主动服务：用户物流卡 3 天，Agent 主动发起会话询问是否需要帮助
- A/B 测试框架：不同 Prompt 模板的效果对比
- 完整可观测性：Langfuse 集成，看每次调用的 token 消耗与延迟