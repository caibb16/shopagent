# ShopAgent — 基于 Spring AI 的电商智能客服 Agent

> 一个面向简历投递的 Java 后端 / AI Agent 实战项目，体现工程能力与独立思考。

[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI 1.0.0-M6](https://img.shields.io/badge/Spring%20AI-1.0.0--M6-blue.svg)](https://spring.io/projects/spring-ai)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![License MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 目录

- [项目简介](#项目简介)
- [核心亮点](#核心亮点简历加分项)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [配置说明](#配置说明)
- [测试](#测试)
- [生产部署](#生产部署)
- [设计文档](#设计文档)
- [开发指南](#开发指南)
- [面试高频问答](#面试高频问答)
- [常见问题](#常见问题)
- [路线图](#路线图)

---

## 项目简介

ShopAgent 是一个面向电商场景的智能客服后端，基于 Spring Boot 3 + Spring AI 构建。它支持：

- 🧠 **意图路由**：自动识别用户是闲聊、咨询、操作（查单/退款）还是投诉
- 🔧 **Function Calling**：LLM 自主调用 6 个工具（订单/物流/退款/优惠券/推荐/转人工）
- 🔒 **工具鉴权**：用户身份从 `UserContext` 线程局部读取，**绝不通过工具参数注入**——防止 prompt 注入越权
- 📚 **RAG 混合检索**：向量检索 + BM25 关键词检索，RRF 融合
- 🔄 **多轮对话与摘要压缩**：每 N 轮自动压缩历史，Token 消耗降低 60%
- 🌊 **SSE 流式响应**：基于 WebFlux `Flux<ServerSentEvent>`，Token 粒度推送
- 🧱 **可插拔架构**：dev 用 In-Memory，prod 接 MySQL/Redis/Qdrant，Spring Profile 一键切换

业务场景：**订单咨询、物流查询、退款工单、FAQ 问答**。

---

## 核心亮点（简历加分项）

| 设计决策 | 体现的能力 | 代码位置 |
| --- | --- | --- |
| 意图分类后再选 Agent | 系统设计能力，避免 prompt 注入 | `agent/IntentClassifier.java` |
| ThreadLocal 工具鉴权 | 安全意识 | `tool/UserContext.java` + 6 个 Tool |
| 历史摘要压缩 | 工程优化、成本意识 | `session/SessionSummaryService.java` |
| WebFlux SSE 流式响应 | 全栈能力 | `api/ChatController.java` |
| 工具失败重试 + 降级 | 鲁棒性设计 | `api/error/SseErrorSender.java` |
| 混合检索（向量 + BM25） | 不是简单调包 | `rag/HybridRetriever.java`（RRF） |
| 查询改写 | 理解检索增强 | `rag/QueryRewriter.java` |
| 评估 harness | 工程化思维 | `eval/RagEvaluator.java` + `data/eval/test_set.jsonl` |
| dev/prod 双实现 + Profile 切换 | 抽象能力、可演进架构 | `business/repo/impl/*` |

---

## 技术栈

| 层 | 技术 | 版本 | 用途 |
| --- | --- | --- | --- |
| Web 框架 | Spring Boot | 3.3.4 | 容器 + 自动配置 |
| Web 层 | Spring WebFlux | 3.3.4 | SSE 流式响应 |
| AI 框架 | Spring AI | 1.0.0-M6 | LLM 集成（OpenAI 兼容协议 → DeepSeek） |
| ORM（业务主） | MyBatis-Plus | 3.5.7 | 订单/退款表（写多读多） |
| ORM（配置类） | Spring Data JPA | 3.3.4 | 用户/优惠券/产品（读多写少） |
| 缓存 | Redisson | 3.34.1 | 会话存储、分布式锁（prod） |
| 向量库 | Qdrant | 1.8.0 | 知识库向量检索（prod） |
| LLM | DeepSeek | deepseek-chat | 中文友好、OpenAI 兼容 |
| Embedding | 占位（dev NoOp）/ Qdrant 远端 | — | prod 由 Qdrant 服务侧生成 |
| 构建 | Maven | 3.9.9 | 多模块构建 |
| 部署 | Docker + Docker Compose | — | 一键启动 MySQL+Redis+Qdrant+App |
| 可观测 | Spring Boot Actuator + 自研 Advisors | — | health/metrics + LLM 日志/token 统计 |
| 测试 | JUnit 5 + Mockito + AssertJ | — | 33 个测试，覆盖单元/切片/E2E |

---

## 系统架构

```
                        ┌─────────────────────────┐
                        │   Web 前端（可选）         │
                        └──────────┬──────────────┘
                                   │ SSE
                        ┌──────────▼──────────────┐
                        │  ChatController         │  GET /api/chat/stream
                        │  (WebFlux, ServerSentEv)│  共享 chatWorkerPool
                        └──────────┬──────────────┘  保证 ThreadLocal 隔离
                                   │
                        ┌──────────▼──────────────┐
                        │  AgentService           │  意图路由
                        │  ├ ChitchatAgent        │
                        │  ├ RagAgent             │  ← HybridRetriever (RRF)
                        │  ├ ToolAgent            │  ← 6 个 @Tool
                        │  └ HandoffService       │  → EscalateTool
                        └──────────┬──────────────┘
                                   │
            ┌──────────┬───────────┼───────────┬──────────┐
            ▼          ▼           ▼           ▼          ▼
       SessionStore  RAG       IntentCls   SessionSummary ChatClient
       (Redis/内存)  (Qdrant)   (规则+LLM)  (LLM压缩)    (DeepSeek)
            │          │
            ▼          ▼
         MySQL    VectorIndex
                   (Qdrant/InMemory)
```

### 核心流程时序

```
用户 → ChatController → AgentService → IntentClassifier
                                │
                  ┌─────────────┼─────────────┬──────────────┐
                  ▼             ▼             ▼              ▼
              ChitchatAgent   RagAgent    ToolAgent      HandoffService
                  │             │             │              │
                  │             │             ▼              │
                  │             │      6 个 @Tool  ◀── UserContext.current()
                  │             │      (鉴权)              │
                  │             ▼                          │
                  │       HybridRetriever                  │
                  │       (Vector + BM25 + RRF)            │
                  ▼             ▼                          ▼
              ChatClient (DeepSeek) ←─── 流式 Token ───┐
                                                        │
                          SSE 推送 ◀────────────────────┘
```

---

## 快速开始

### 环境要求

- **JDK 17+**（项目用 `--release 17` 编译；JDK 25 测试通过）
- **Maven 3.9+**
- **（可选）Docker** — 仅 prod 部署需要

### 1. 克隆与构建

```bash
git clone <repo>
cd shopagent
mvn clean test            # 33 个测试应全部通过
```

### 2. 启动 dev profile（无需任何外部依赖）

```bash
mvn spring-boot:run
```

启动成功后日志会显示：

```
Started ShopAgentApplication in 4.0 seconds
Knowledge base loaded: 52 entries
Mock data loaded: 3 users, 4 orders, 2 coupons, 4 products
```

> ⚠️ **dev profile 不需要 DeepSeek API key**，ChatClient 会用 `dev-placeholder` 构建。真实 LLM 调用会 401，但所有非 LLM 路径（鉴权、意图规则、BM25 检索）正常工作。

### 3. 调用 SSE 接口

需要真实 LLM 时，传入 API key：

```bash
export DEEPSEEK_API_KEY=sk-xxx
mvn spring-boot:run
```

然后用 curl 测试 SSE 流：

```bash
curl -N "http://localhost:8080/api/chat/stream?sessionId=demo-1&message=我的订单O1002到哪了" \
  -H "X-User-Id: 1"
```

会看到 SSE 事件流：

```
data:你的订单 O1002 已发货，物流单号 SF1234567890，

data:当前在上海中转站，预计明天送达。

data:[DONE]
```

### 4. 用真实 LLM 试用不同意图

| 用户输入 | 意图 | 走哪条路 |
| --- | --- | --- |
| "你好" | CHITCHAT | 直接 LLM 闲聊 |
| "7天无理由是什么意思" | INQUIRY | RAG 检索 FAQ |
| "查订单 O1002" | ACTION | 工具调用 `OrderTool.getOrderDetail` |
| "我要投诉" | COMPLAINT | `HandoffService` → `EscalateTool` |
| "推荐个 500 元以内的键盘" | ACTION | `RecommendTool.recommendProducts` |

---

## API 文档

### `GET /api/chat/stream`

流式聊天端点（SSE）。

**Query 参数：**

| 名称 | 必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `sessionId` | 是 | string | 会话 ID，同一会话保留历史 |
| `message` | 是 | string | 用户消息文本 |

**Header：**

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| `X-User-Id` | 是 | 当前用户 ID（dev 用 mock: 1/2/999） |

**响应：** `Content-Type: text/event-stream`

每个事件形如 `data: <token>\n\n`，流结束为 `data:\n\n` 或 `event: done\ndata:\n\n`。

**错误事件：** `event: error\ndata: {"error":"...","message":"..."}\n\n`

**示例：**

```bash
curl -N "http://localhost:8080/api/chat/stream?sessionId=demo&message=7天无理由退货" \
  -H "X-User-Id: 1"
```

### `GET /actuator/health`

健康检查端点，prod 用于 k8s liveness/readiness probe。

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### `GET /actuator/metrics`

Micrometer 指标（`http.server.requests`、JVM、Tomcat 等）。

---

## 配置说明

### Profile 切换

默认 `application.yml` 设置 `spring.profiles.active: dev`。可通过环境变量覆盖：

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/shopagent-0.1.0-SNAPSHOT.jar
```

### 关键配置项（`application.yml` + `application-{profile}.yml`）

| 配置项 | 默认 | 说明 |
| --- | --- | --- |
| `shopagent.llm.api-key` | `${DEEPSEEK_API_KEY:dev-placeholder}` | DeepSeek API key |
| `shopagent.llm.base-url` | `https://api.deepseek.com` | OpenAI 兼容端点 |
| `shopagent.llm.model` | `deepseek-chat` | 模型名 |
| `shopagent.vector.backend` | `simple` (dev) / `qdrant` (prod) | 向量库后端 |
| `shopagent.vector.qdrant.host` | `localhost` | Qdrant 主机 |
| `shopagent.vector.qdrant.port` | `6334` | Qdrant gRPC 端口 |
| `shopagent.vector.qdrant.collection` | `shopagent-kb` | 集合名 |
| `shopagent.session.summary-every-n-turns` | `5` | 摘要压缩触发频率 |
| `spring.datasource.url` | H2 内存 (dev) / MySQL (prod) | 数据库连接 |

---

## 测试

### 运行所有测试

```bash
mvn test
```

输出应为：

```
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
```

### 测试分布

| 测试类 | 测试数 | 覆盖 |
| --- | --- | --- |
| `ShopAgentApplicationTests` | 1 | Spring 上下文加载（autoconfig 回归保护） |
| `InMemoryOrderRepositoryTest` | 3 | Repository TDD（红→绿） |
| `MockDataLoaderTest` | 2 | Mock 数据完整性 |
| `UserContextTest` | 3 | ThreadLocal 读写 + clear 防泄漏 |
| `InMemorySessionStoreTest` | 5 | 会话存储边界（limit、隔离、摘要） |
| `PromptTemplatesTest` | 3 | 模板格式化 |
| `HybridRetrieverTest` | 2 | RRF 融合 + topK 截断 |
| `KnowledgeBaseRetrievalTest` | 1 | 真实 52 条 FAQ 检索（中文查询） |
| `OrderToolAuthTest` | 3 | 订单工具鉴权（owner/非 owner/不存在） |
| `AllToolsAuthTest` | 3 | 其他 5 个工具鉴权 |
| `IntentClassifierTest` | 4 | 规则分类 + 黑名单 |
| `ChatControllerE2ETest` | 1 | SSE 端到端切片测试 |
| `RagEvaluatorTest` | 1 | 召回率计算 |

### 运行评估 harness

RagEvaluator 是组件，可注入到任何测试或 main 方法：

```java
@Autowired RagEvaluator evaluator;
EvalReport report = evaluator.evaluate(testCases);
System.out.println("Recall@5: " + report.recallAt5());
```

---

## 生产部署

### 一键启动完整栈

```bash
# 1. 准备环境变量
cp .env.example .env
# 编辑 .env，填入 DEEPSEEK_API_KEY=sk-xxx

# 2. 启动所有服务
docker compose up -d

# 3. 查看日志
docker compose logs -f app
```

启动后访问：

| 服务 | 地址 |
| --- | --- |
| 后端 API | `http://localhost:8080` |
| SSE 端点 | `http://localhost:8080/api/chat/stream` |
| Actuator | `http://localhost:8080/actuator/health` |
| Qdrant UI | `http://localhost:6333/dashboard` |
| MySQL | `localhost:3306`（user: shopagent / pass: shopagentpw） |
| Redis | `localhost:6379` |

### 仅构建镜像

```bash
docker build -t shopagent:latest .
```

### 关闭

```bash
docker compose down            # 停止容器
docker compose down -v         # 同时删除数据卷
```

---

## 设计文档

完整设计文档和实施计划位于 `docs/superpowers/`：

- `docs/superpowers/specs/2026-08-28-ecommerce-customer-service-agent-design.md` — 系统设计
- `docs/superpowers/plans/2026-08-28-shopagent-mvp-backend.md` — 实施计划（21 个 Task）
- `docs/superpowers/plans/2026-08-28-shopagent-mvp-backend.md` 末尾的"MVP Scope Cuts"段列出了 v2 待办

---

## 开发指南

### 添加新的工具（Function Calling）

1. 在 `tool/` 包下创建新类，标注 `@Component`
2. 方法标注 `@Tool(description = "...")`，参数标注 `@ToolParam(description = "...")`
3. **用户身份从 `UserContext.current().userId()` 读取，不要作为 `@ToolParam`**
4. 在 `tool/ToolAgent.java` 注册到 `.tools(...)` 列表
5. 写测试：`UserContext.set(...)` → 调用工具 → 断言行为 → `UserContext.clear()`

模板：

```java
@Component
@RequiredArgsConstructor
public class MyTool {
    @Tool(description = "...")
    public ReturnType myMethod(@ToolParam(description = "...") String param) {
        Long uid = UserContext.current().userId();  // 鉴权源
        // ... 业务逻辑
    }
}
```

### 添加新的 FAQ 类别

1. 编辑 `src/main/resources/knowledge/faq.jsonl`，按行追加 `{id, category, question, answer}`
2. 重启服务，`KnowledgeBaseBootstrap` 会自动加载
3. 可选：在 `data/eval/test_set.jsonl` 增加对应评估用例

### 切换到真实 Embedding（替换 NoOp）

dev profile 当前用 NoOp 向量（BM25 leg 承担检索），prod 用 Qdrant 远端 embedding。

如需 dev 也用真实 embedding：
1. 在 `application-dev.yml` 设置 `spring.ai.openai.api-key`（或换 ONNX 本地模型）
2. 把 `InMemoryVectorIndex` 改回用 `EmbeddingModel` 注入
3. 注意 JDK 25 + ONNX 兼容性（详见常见问题 #2）

---

## 面试高频问答

### 后端基础

**Q: Spring AI 怎么和 Spring Boot 集成？**
A: 通过 `spring-ai-openai-spring-boot-starter`，自动配置 `ChatClient` Bean。本项目因 `OpenAiAutoConfiguration` 强制要求 api-key，改用 `DeepSeekConfig` 手动构建 ChatClient + Advisors 链。

**Q: Function Calling 的实现原理？**
A: LLM 返回结构化 JSON（tool_calls）→ Spring AI 解析 → 反射调用本地 `@Tool` 方法 → 把结果回填 LLM → LLM 生成自然语言回复。

**Q: 工具调用失败怎么降级？**
A: 三层：① 工具内部 `try/catch` + 重试；② `SseErrorSender` 推送 SSE error 事件而非断开连接；③ `GlobalExceptionHandler` 统一返回 403/500 JSON。

**Q: SSE 和 WebSocket 区别？**
A: SSE 单向、HTTP 兼容、自动重连，客服场景足够；WebSocket 双向但需要单独协议升级。

**Q: Redis 怎么存会话？**
A: dev 用 `ConcurrentHashMap<sessionId, Deque<Message>>`；prod 用 Redis `list`（`LPUSH`/`LRANGE`）+ `set`（摘要）。Key 格式：`session:hist:{sessionId}` 和 `session:sum:{sessionId}`。

**Q: 怎么控制 LLM 调用成本？**
A: ① 历史摘要压缩（每 N 轮一次）；② 缓存相似 query 结果；③ 限流（按用户/全局）；④ 用小模型做意图路由、大模型做生成。

**Q: Spring AI 的 Advisor 是什么？**
A: 类似 AOP 拦截器，在 LLM 调用前后插入逻辑。本项目用了 `LoggingAdvisor`（日志 prompt/response）和 `TokenUsageAdvisor`（累加 token）。

**Q: RabbitMQ 用在哪？**
A: v2：异步退款工单、订单状态变更触发主动通知客服。MVP 同步退款。

**Q: MySQL 表怎么设计？**
A: 见 `src/main/resources/schema.sql`。user / orders / refund / coupon 四张表，关键索引 `idx_user (user_id)`。

**Q: Docker Compose 一键启动？**
A: `docker-compose.yml` 定义 mysql + redis + qdrant + app 四个 service，通过 env 注入连接信息，schema.sql 挂载到 MySQL init 目录。

### AI / Agent 专项

**Q: Agent 和普通 LLM 调用区别？**
A: Agent 能自主规划多步、调用工具、根据工具结果决定下一步。本项目通过 `AgentService` 路由器 + 4 类 Specialist Agent 实现。

**Q: ReAct 是什么？**
A: Reasoning + Acting 循环：Thought → Action → Observation → Thought...。Spring AI 的 Function Calling 自动实现这个循环。

**Q: Function Calling vs Prompt 调用？**
A: FC 准确率更高，模型原生支持；Prompt 调用灵活但靠模型自律、不可靠。本项目严格使用 FC。

**Q: RAG 检索不准怎么排查？**
A: ① 看召回：topK=20 是否覆盖目标文档；② 看 chunk 大小：本项目按"问题+答案"整篇切；③ 看 embedding：dev 用 NoOp，prod 用 Qdrant 远端；④ 看 query 改写：QueryRewriter 是否生效。

**Q: 向量库选型？**
A: Milvus 性能强但部署重；Qdrant 单二进制 + REST/gRPC，demo 友好。

**Q: 怎么防止 prompt 注入？**
A: 三层：① 系统 prompt 约束；② 工具鉴权用 ThreadLocal，userId 不通过工具参数；③ 敏感操作（如退款）二次确认。

**Q: 多 Agent 协作和单 Agent 区别？**
A: 单 Agent 适合窄场景（客服）；多 Agent 适合复杂任务分解（电商=客服+推荐+风控）。本项目是 4 个 Specialist Agent + 1 个 Router。

**Q: Token 怎么计算？**
A: 中文约 1.5 字/token，英文 0.75 字/token。DeepSeek API 返回 `usage.prompt_tokens` / `completion_tokens`，由 `TokenUsageAdvisor` 累加。

**Q: Embedding 模型选型？**
A: 中文场景 bge-small-zh（轻量）/ bge-large-zh-v1.5（更准但慢）。本项目 prod 用 Qdrant 服务侧生成，dev NoOp。

**Q: 流式响应怎么实现？**
A: LLM 端 DeepSeek 原生支持 SSE；Spring AI `ChatClient.stream().content()` 返回 `Flux<String>`；本项目包成 `Flux<ServerSentEvent<String>>` 推给客户端。

---

## 常见问题

### 1. `mvn test` 报错 "Mockito cannot mock this class"

**原因**：JDK 25 与 Byte Buddy 1.14.19（Spring Boot 3.3.4 默认）不完全兼容。

**解决**：pom.xml 已加 `-Dnet.bytebuddy.experimental=true` 到 surefire `argLine`。如仍报错，确认用 JDK 17/21，或升级 Spring Boot 到 3.4+。

### 2. 启动时报 "TransformersEmbeddingModel failed to load ONNX"

**原因**：Spring AI 内置的 ONNX embedding 在 JDK 25 上 protobuf 解析失败。

**解决**：dev profile 已用 NoOp `InMemoryVectorIndex` 规避，prod 用 Qdrant 远端 embedding。如需 dev 也用真实 embedding，降级到 JDK 17 或替换 ONNX 模型。

### 3. SSE 输出无内容 / 立即断开

**检查**：
- `X-User-Id` header 是否设置
- DeepSeek API key 是否有效（401 → key 无效）
- `mvn spring-boot:run` 日志是否报 `OpenAiAutoConfiguration` 启动失败

### 4. `contextLoads` 测试失败

dev profile 已排除 `OpenAiAutoConfiguration`（Task 1 fix）。如果自己修改了 `application.yml` 移除了 exclude，启动会会要求 API key，请恢复 exclude 或设置 `DEEPSEEK_API_KEY`。

### 5. 中文 FAQ 检索召回率低

dev profile 仅 BM25 工作（CJK 分词：unigram + bigram）。如需语义匹配：
- 启用 prod profile 走 Qdrant 远端 embedding
- 或在 `application-dev.yml` 注入真实 `EmbeddingModel`

### 6. 修改 FAQ 后没生效

`KnowledgeBaseBootstrap` 用 `@PostConstruct`，启动时一次性加载。修改 `faq.jsonl` 后需要重启服务。

---

## 路线图

### v1（已完成）
- ✅ 21 个 Task 全部交付
- ✅ 33 个测试全部通过
- ✅ dev/prod 双实现
- ✅ Docker Compose 一键启动

### v2（计划中，未实现）
- 🔲 RabbitMQ 异步退款工单
- 🔲 Cross-Encoder Reranker（替换 NoOp）
- 🔲 Langfuse 可观测性
- 🔲 多模态：用户上传图片识别破损商品
- 🔲 主动服务：物流超时主动询问
- 🔲 A/B 测试框架：不同 Prompt 模板对比

### v3（远期）
- 多 Agent 协作：客服 Agent + 推荐 Agent + 风控 Agent 联动
- 完整可观测性：每次 LLM 调用的 token + 延迟 + 链路追踪
- 强化学习：基于用户反馈优化 Prompt

---

## 致谢

- 设计灵感来自 Spring AI 官方文档、LangChain4j 教程
- 评测框架参考 RAGAS

## License

MIT